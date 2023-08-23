package com.apzda.cloud.gsvc.core;

import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import com.apzda.cloud.gsvc.dto.CurrentUser;
import com.apzda.cloud.gsvc.dto.UploadFile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.MethodDescriptor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bouncycastle.util.Strings;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.util.*;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.apzda.cloud.gsvc.ResponseUtils.OBJECT_MAPPER;

/**
 * 本地服务处理器.
 *
 * @author ninggf
 */
@Slf4j

public class GatewayServiceHandler implements Supplier<DataBuffer> {

    private final GatewayServiceRegistry.MethodInfo methodInfo;
    private final ObjectMapper objectMapper;
    private final ServerWebExchange exchange;
    private final List<Tuple2<File, FilePart>> fileContents = new ArrayList<>();
    private final String logId;
    private final GatewayServiceConfigure svcConfigure;

    private GatewayServiceHandler(GatewayServiceRegistry.MethodInfo methodInfo,
                                  ServerWebExchange exchange, ApplicationContext applicationContext) {
        this.methodInfo = methodInfo;
        this.objectMapper = OBJECT_MAPPER;
        this.exchange = exchange;
        svcConfigure = applicationContext.getBean(GatewayServiceConfigure.class);
        logId = exchange.getRequiredAttribute(ServerWebExchange.LOG_ID_ATTRIBUTE);
    }

    /**
     * 调用本地方法.
     *
     * @param methodInfo         要调用的方法.
     * @param exchange           交换机
     * @param applicationContext BeanFactory
     * @return 调用结果
     */
    public static CompletableFuture<DataBuffer> handle(GatewayServiceRegistry.MethodInfo methodInfo,
                                                       ServerWebExchange exchange,
                                                       ApplicationContext applicationContext) {
        // unary method
        return CompletableFuture.supplyAsync(new GatewayServiceHandler(methodInfo, exchange, applicationContext));
    }

    /**
     * 调用本地方法(异步).
     *
     * @param methodInfo         要调用的方法.
     * @param exchange           交换机
     * @param applicationContext BeanFactory
     * @return 调用结果
     */
    public static Mono<DataBuffer> handleAsync(GatewayServiceRegistry.MethodInfo methodInfo,
                                               ServerWebExchange exchange,
                                               ApplicationContext applicationContext

    ) {
        try {
            val handler = new GatewayServiceHandler(methodInfo, exchange, applicationContext);
            val type = methodInfo.getType();
            if (type.equals(MethodDescriptor.MethodType.SERVER_STREAMING)) {
                return handler.forServerStreamingMethod();
            } else if (type.equals(MethodDescriptor.MethodType.BIDI_STREAMING)) {
                return handler.forBiStreamingMethod();
            }
            //bookmark: 服务异常处理
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
        } catch (Exception e) {
            //bookmark: 服务异常处理
            return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @Override
    public DataBuffer get() {
        try {
            //bookmark: unary method handler
            // 此处运行在独立的线程中需要设置sa-token的上下文
            SaReactorSyncHolder.setContext(exchange);
            val resp = methodInfo.call(deserializeRequest(exchange));
            return createResponseDataBuffer(resp);
        } catch (Exception e) {
            log.error("[{}] Calling {} failed: ", logId, methodInfo, e);
            //bookmark: 服务异常处理
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            SaReactorSyncHolder.clearContext();
        }
    }

    @SuppressWarnings("unchecked")
    private Mono<DataBuffer> forServerStreamingMethod() {
        //bookmark: server-streaming method handler
        return Mono.empty().publish((aVoid) -> {
                try {
                    Object request = deserializeRequest(exchange);
                    return (Mono<Object>) methodInfo.call(request);
                } catch (IOException | IllegalAccessException | InvocationTargetException e) {
                    return Mono.error(e);
                }
            }).subscribeOn(Schedulers.boundedElastic()) //tbd: 是否要使用自定义线程池?
            .handle((resp, sink) -> {
                try {
                    sink.next(createResponseDataBuffer(resp));
                    sink.complete();
                } catch (JsonProcessingException e) {
                    sink.error(e);
                }
            });
    }

    @SuppressWarnings("unchecked")
    private Mono<DataBuffer> forBiStreamingMethod() {
        //bookmark: bi-streaming method handler
        return Mono.fromFuture(CompletableFuture.supplyAsync(() -> {
                try {
                    SaReactorSyncHolder.setContext(exchange);
                    return deserializeRequest(exchange);
                } catch (IOException e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
                } finally {
                    SaReactorSyncHolder.clearContext();
                }
            }))
            .publish(request -> {
                try {
                    return (Mono<Object>) methodInfo.call(request);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    return Mono.error(e);
                }
            })
            .handle((resp, sink) -> {
                try {
                    sink.next(createResponseDataBuffer(resp));
                    sink.complete();
                } catch (JsonProcessingException e) {
                    sink.error(e);
                }
            });
    }

    @SuppressWarnings("rawtypes")
    private Object deserializeRequest(ServerWebExchange exchange) throws IOException {
        val request = exchange.getRequest();
        val contentType = request.getHeaders().getContentType();

        if (contentType == null) {
            log.debug("[{}] Content-Type 'null' is not acceptable", logId);
            exchange.getResponse().setStatusCode(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            throw new UnsupportedMediaTypeException("Content-Type 'null' is not acceptable");
        }

        Object requestObj = null;
        Map args = null;

        val serviceIndex = methodInfo.getServiceIndex();
        val reqClass = methodInfo.reqClass();
        val dmName = methodInfo.getDmName();
        val readTimeout = svcConfigure.getTimeout(serviceIndex, dmName);

        if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            val requestBody = request.getBody().map(this::getByteBuf);
            val stringBuilder = new StringBuilder();

            Objects.requireNonNull(requestBody.collectList().block(readTimeout))
                .forEach((bf) -> stringBuilder.append(bf.toString(StandardCharsets.UTF_8)));

            if (methodInfo.getCurrentUserClz() == null) {
                return objectMapper.readValue(stringBuilder.toString(), reqClass);
            }

            args = objectMapper.readValue(stringBuilder.toString(), Map.class);
        } else {
            MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
            Mono<MultiValueMap<String, String>> formData = exchange.getFormData();
            Mono<MultiValueMap<String, Part>> multipartData = exchange.getMultipartData();
            args = Mono.zip(Mono.just(queryParams), formData, multipartData)
                .map(tuple -> {
                    Map<String, Object> result = new HashMap<>();
                    tuple.getT1().forEach((key, values) -> addBindValue(result, key, values));
                    tuple.getT2().forEach((key, values) -> addBindValue(result, key, values));
                    tuple.getT3().forEach((key, values) -> addBindValue(result, key, values));
                    return result;
                }).block(readTimeout);
        }

        if (args != null) {
            if (methodInfo.getCurrentUserClz() != null && StpUtil.isLogin()) {
                injectCurrentUser(args);
            }
            //文件上传
            if (!fileContents.isEmpty()) {
                Duration uploadTimeout = svcConfigure.getUploadTimeout(serviceIndex, dmName);
                val stopWatch = new StopWatch("处理上传文件");
                stopWatch.start();
                Flux.fromIterable(fileContents)
                    .parallel()
                    .map((tuple) -> tuple.getT2().transferTo(tuple.getT1()))
                    .doOnError((err) -> log.error("[{}] 上传文件失败: {}", logId, err.getMessage()))
                    .runOn(Schedulers.boundedElastic())
                    .then()
                    .block(uploadTimeout);
                stopWatch.stop();
                if (log.isTraceEnabled()) {
                    log.trace("[{}] {}", logId, stopWatch.shortSummary());
                }
            }
            requestObj = objectMapper.readValue(objectMapper.writeValueAsBytes(args), reqClass);
        }

        if (requestObj != null && log.isTraceEnabled()) {
            log.trace("[{}] Request resolved: {}", logId, objectMapper.writeValueAsString(requestObj));
        }
        return requestObj;
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    private void injectCurrentUser(Map args) {
        val currentUser = new CurrentUser();
        val tokenInfo = StpUtil.getTokenInfo();
        currentUser.setUid(tokenInfo.getLoginId().toString());
        args.put("currentUser", currentUser);
    }

    private DataBuffer createResponseDataBuffer(Object resp) throws JsonProcessingException {
        Assert.notNull(resp, "Response Object cannot be null!");
        val bufferFactory = DefaultDataBufferFactory.sharedInstance;
        val respStr = objectMapper.writeValueAsString(resp);
        if (log.isTraceEnabled()) {
            log.trace("[{}] response of {} is: {}", logId, methodInfo, StringUtils.truncate(respStr, 256));
        }
        return bufferFactory.wrap(Strings.toUTF8ByteArray(respStr));
    }

    private ByteBuf getByteBuf(DataBuffer dataBuffer) {
        if (dataBuffer instanceof NettyDataBuffer buffer) {
            return buffer.getNativeBuffer();
        } else if (dataBuffer instanceof DefaultDataBuffer buffer) {
            return Unpooled.wrappedBuffer(buffer.getNativeBuffer());
        }
        throw new IllegalArgumentException("Unable to handle DataBuffer of type " + dataBuffer.getClass());
    }

    private void addBindValue(Map<String, Object> params, String key, List<?> values) {
        if (!CollectionUtils.isEmpty(values)) {
            values = values.stream()
                .map(value -> {
                    if (value instanceof FilePart filePart) {
                        val headers = filePart.headers();
                        File tmpFile = null;
                        try {
                            tmpFile = File.createTempFile("UP_LD_", ".part");
                        } catch (IOException e) {
                            //bookmark: 服务异常处理
                            throw new RuntimeException(e);
                        }

                        var file = UploadFile.builder()
                            .file(tmpFile.getAbsolutePath())
                            .name(filePart.name())
                            .ext(FileUtil.extName(filePart.filename()))
                            .filename(filePart.filename())
                            .contentType(Optional.ofNullable(headers.getContentType())
                                .orElse(MediaType.TEXT_PLAIN)
                                .toString());

                        val finalTmpFile = tmpFile;
                        if (log.isTraceEnabled()) {
                            log.trace("[{}] 文件'{}'将上传到: {}",
                                logId,
                                filePart.filename(),
                                tmpFile.getAbsoluteFile());
                        }
                        fileContents.add(Tuples.of(finalTmpFile, filePart));
                        return file.build();
                    } else if (value instanceof FormFieldPart formFieldPart) {
                        return formFieldPart.value();
                    } else {
                        return value;
                    }
                }).toList();

            params.put(key, values.size() == 1 ? values.get(0) : values);
        }
    }
}
