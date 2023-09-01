package com.apzda.cloud.gsvc.core;

import cn.hutool.core.io.FileUtil;
import com.apzda.cloud.gsvc.ResponseUtils;
import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.dto.UploadFile;
import com.apzda.cloud.gsvc.exception.handler.GsvcExceptionHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
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

    private final GatewayServiceRegistry.ServiceMethod serviceMethod;

    private final ObjectMapper objectMapper;

    private final List<Tuple2<File, FilePart>> fileContents = new ArrayList<>();

    private final GsvcExceptionHandler exceptionHandler;

    private String logId;

    public ServiceMethodHandler(ServerRequest request, GatewayServiceRegistry.ServiceMethod serviceMethod,
            ApplicationContext applicationContext) {
        this.request = request;
        this.serviceMethod = serviceMethod;
        svcConfigure = applicationContext.getBean(GatewayServiceConfigure.class);
        exceptionHandler = applicationContext.getBean(GsvcExceptionHandler.class);
        objectMapper = ResponseUtils.OBJECT_MAPPER;
    }

    public static ServerResponse handle(ServerRequest request, GatewayServiceRegistry.ServiceMethod serviceMethod,
            ApplicationContext applicationContext) {
        val mInfo = GatewayServiceRegistry.fromDeclaredMethod(serviceMethod);

        return new ServiceMethodHandler(request, mInfo, applicationContext).run();
    }

    private ServerResponse run() {
        try {
            logId = GsvcContextHolder.getRequestId();
            if (log.isTraceEnabled()) {
                log.trace("[{}] Start to call method: {}@{}/{}", logId, serviceMethod.getServiceName(),
                        serviceMethod.getAppName(), serviceMethod.getDmName());
            }
            // 1. 解析请求体
            Object requestObj = deserializeRequest();
            // 2. 调用方法
            val type = serviceMethod.getType();

            return switch (type) {
                case UNARY -> doUnaryCall(requestObj);
                case SERVER_STREAMING -> doStreamingCall(requestObj);
                default -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                }
            };
        }
        catch (Exception e) {
            log.error("[{}] Start to call method: {}@{}/{}", logId, serviceMethod.getServiceName(),
                    serviceMethod.getAppName(), serviceMethod.getDmName(), e);
            return exceptionHandler.handle(e, request);
        }
    }

    @SuppressWarnings("unchecked")
    private ServerResponse doStreamingCall(Object requestObj) throws InvocationTargetException, IllegalAccessException {
        // 仅支持Mono
        val mono = (Mono<Object>) serviceMethod.call(requestObj);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono);
    }

    private ServerResponse doUnaryCall(Object requestObj)
            throws InvocationTargetException, IllegalAccessException, JsonProcessingException {
        val resp = serviceMethod.call(requestObj);
        val response = createResponse(resp);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(response);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object deserializeRequest() throws IOException, ServletException {
        val contentType = request.headers().contentType().orElse(MediaType.APPLICATION_FORM_URLENCODED);

        Object requestObj = null;
        Map args = null;

        val serviceIndex = serviceMethod.getServiceIndex();
        val reqClass = serviceMethod.reqClass();
        val dmName = serviceMethod.getDmName();
        val readTimeout = svcConfigure.getTimeout(serviceIndex, dmName);

        if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            val requestBody = retrieveRequestBody(request.servletRequest());
            log.trace("[{}] Request resolved: {}", logId, requestBody);

            return objectMapper.readValue(requestBody, reqClass);
        }
        else if (contentType.isCompatibleWith(MediaType.MULTIPART_FORM_DATA)
                || contentType.isCompatibleWith(MediaType.MULTIPART_MIXED)) {
            MultiValueMap<String, String> queryParams = request.params();
            val multipartData = Mono.just(request.multipartData());
            args = Mono.zip(Mono.just(queryParams), multipartData).map(tuple -> {
                Map<String, Object> result = new HashMap<>();
                tuple.getT1().forEach((key, values) -> addBindValue(result, key, values));
                tuple.getT2().forEach((key, values) -> addBindValue(result, key, values));
                return result;
            }).block(readTimeout);
        }
        else {
            // 此处真的是
            args = new HashMap<String, String>();
            for (Map.Entry<String, List<String>> kv : request.params().entrySet()) {
                args.put(kv.getKey(), kv.getValue().get(0));
            }
        }

        if (args != null) {
            // 文件上传
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

    private void addBindValue(Map<String, Object> params, String key, List<?> values) {
        if (!CollectionUtils.isEmpty(values)) {
            values = values.stream().map(value -> {
                if (value instanceof FilePart filePart) {
                    val headers = filePart.headers();
                    File tmpFile = null;
                    try {
                        tmpFile = File.createTempFile("UP_LD_", ".part");
                    }
                    catch (IOException e) {
                        // bookmark: 服务异常处理
                        throw new RuntimeException(e);
                    }

                    var file = UploadFile.builder()
                        .file(tmpFile.getAbsolutePath())
                        .name(filePart.name())
                        .ext(FileUtil.extName(filePart.filename()))
                        .filename(filePart.filename())
                        .contentType(
                                Optional.ofNullable(headers.getContentType()).orElse(MediaType.TEXT_PLAIN).toString());

                    val finalTmpFile = tmpFile;
                    if (log.isTraceEnabled()) {
                        log.trace("[{}] 文件'{}'将上传到: {}", logId, filePart.filename(), tmpFile.getAbsoluteFile());
                    }
                    fileContents.add(Tuples.of(finalTmpFile, filePart));
                    return file.build();
                }
                else if (value instanceof FormFieldPart formFieldPart) {
                    return formFieldPart.value();
                }
                else {
                    return value;
                }
            }).toList();

            params.put(key, values.size() == 1 ? values.get(0) : values);
        }
    }

    private String createResponse(Object resp) throws JsonProcessingException {
        val respStr = objectMapper.writeValueAsString(resp);
        if (log.isTraceEnabled()) {
            log.trace("[{}] Response of {}@{}/{} is: {}", logId, serviceMethod.getServiceName(), serviceMethod.getAppName(),
                    serviceMethod.getDmName(), StringUtils.truncate(respStr, 256));
        }
        return respStr;
    }

    private String retrieveRequestBody(HttpServletRequest request) {
        val req = new ContentCachingRequestWrapper(request);
        try {
            val reader = req.getReader();
            val stringBuilder = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                stringBuilder.append(line);
                line = reader.readLine();
            }
            return stringBuilder.toString();
        }
        catch (IOException e) {
            log.error("[{}] Retrieve request body failed: {}", logId, e.getMessage());
            return null;
        }
    }

}
