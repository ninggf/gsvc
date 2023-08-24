package com.apzda.cloud.gsvc.core;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import com.apzda.cloud.gsvc.ResponseUtils;
import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.dto.CurrentUser;
import com.apzda.cloud.gsvc.dto.UploadFile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.util.*;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.*;

/**
 * @author ninggf
 */
@Slf4j
public class ServiceMethodHandler {
    private final ServerRequest request;
    private final GatewayServiceConfigure svcConfigure;
    private final GatewayServiceRegistry.MethodInfo methodInfo;
    private final ObjectMapper objectMapper;
    private final List<Tuple2<File, FilePart>> fileContents = new ArrayList<>();
    private final String logId;
    private final SaTokenExtendProperties properties;

    public ServiceMethodHandler(ServerRequest request,
                                GatewayServiceRegistry.MethodInfo methodInfo,
                                ApplicationContext applicationContext) {
        this.request = request;
        this.methodInfo = methodInfo;
        properties = applicationContext.getBean(SaTokenExtendProperties.class);
        svcConfigure = applicationContext.getBean(GatewayServiceConfigure.class);
        objectMapper = ResponseUtils.OBJECT_MAPPER;
        logId = request.headers().firstHeader("X-Request-Id");
    }

    public static ServerResponse handle(ServerRequest request,
                                        GatewayServiceRegistry.MethodInfo methodInfo,
                                        ApplicationContext applicationContext
    ) {
        val mInfo = GatewayServiceRegistry.getServiceMethod(methodInfo);
        return new ServiceMethodHandler(request, mInfo, applicationContext).run();
    }

    private ServerResponse run() {
        try {
            // 1. 解析请求体
            Object requestObj = deserializeRequest();
            // 2. 调用方法
            val type = methodInfo.getType();
            return switch (type) {
                case UNARY -> doUnaryCall(requestObj);
                case SERVER_STREAMING, BIDI_STREAMING -> doStreamingCall(requestObj);
                default -> ServerResponse.status(HttpStatus.NOT_ACCEPTABLE).build();
            };
        } catch (IOException | ServletException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NotLoginException nle) {
            if (log.isTraceEnabled()) {
                log.trace("用户未登录: {}", nle.getMessage());
            }
            val contentTypes = request.headers().accept();
            val loginUrl = properties.getLoginUrl();
            val textType = MediaType.parseMediaType("text/*");
            if (loginUrl != null && !CollectionUtils.isEmpty(contentTypes)) {
                for (MediaType contentType : contentTypes) {
                    if (contentType.isCompatibleWith(textType)) {
                        // redirect to the login page
                        return ServerResponse.status(HttpStatus.TEMPORARY_REDIRECT).location(loginUrl).build();
                    }
                }
            }
            return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
        }
    }


    private ServerResponse doStreamingCall(Object requestObj) throws InvocationTargetException, IllegalAccessException {
        val mono = (Mono<Object>) methodInfo.call(requestObj);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono);
    }

    private ServerResponse doUnaryCall(Object requestObj) throws
                                                          InvocationTargetException,
                                                          IllegalAccessException,
                                                          JsonProcessingException {
        val resp = methodInfo.call(requestObj);
        val response = createResponse(resp);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(response);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object deserializeRequest() throws IOException, ServletException {
        if (methodInfo.getCurrentUserClz() != null) {
            StpUtil.checkLogin();
        }

        val contentType = request.headers().contentType().orElse(MediaType.APPLICATION_FORM_URLENCODED);

        Object requestObj = null;
        Map args = null;

        val serviceIndex = methodInfo.getServiceIndex();
        val reqClass = methodInfo.reqClass();
        val dmName = methodInfo.getDmName();
        val readTimeout = svcConfigure.getTimeout(serviceIndex, dmName);

        if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            val requestBody = request.body(String.class);
            if (methodInfo.getCurrentUserClz() == null) {
                return objectMapper.readValue(requestBody, reqClass);
            }

            args = objectMapper.readValue(requestBody, Map.class);
        } else if (contentType.isCompatibleWith(MediaType.MULTIPART_FORM_DATA) || contentType.isCompatibleWith(MediaType.MULTIPART_MIXED)) {
            MultiValueMap<String, String> queryParams = request.params();
            val multipartData = Mono.just(request.multipartData());
            args = Mono.zip(Mono.just(queryParams), multipartData)
                .map(tuple -> {
                    Map<String, Object> result = new HashMap<>();
                    tuple.getT1().forEach((key, values) -> addBindValue(result, key, values));
                    tuple.getT2().forEach((key, values) -> addBindValue(result, key, values));
                    return result;
                }).block(readTimeout);
        } else {
            // 此处真的是
            args = new HashMap<String, String>();
            for (Map.Entry<String, List<String>> kv : request.params().entrySet()) {
                args.put(kv.getKey(), kv.getValue().get(0));
            }
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

    private String createResponse(Object resp) throws JsonProcessingException {
        Assert.notNull(resp, "Response Object cannot be null!");
        val respStr = objectMapper.writeValueAsString(resp);
        if (log.isTraceEnabled()) {
            log.trace("[{}] response of {} is: {}", logId, methodInfo, StringUtils.truncate(respStr, 256));
        }
        return respStr;
    }
}
