package com.apzda.cloud.gsvc.core;

import cn.hutool.core.io.FileUtil;
import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.dto.UploadFile;
import com.apzda.cloud.gsvc.exception.handler.GsvcExceptionHandler;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.MethodDescriptor;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING;

/**
 * @author ninggf
 */
@Slf4j
public class ServiceMethodHandler {

    private final ServerRequest request;

    private final GatewayServiceConfigure svcConfigure;

    private final ServiceMethod serviceMethod;

    private final ObjectMapper objectMapper;

    private final List<Tuple2<File, MultipartFile>> fileContents = new ArrayList<>();

    private final GsvcExceptionHandler exceptionHandler;

    private String logId;

    public ServiceMethodHandler(ServerRequest request, ServiceMethod serviceMethod,
            ApplicationContext applicationContext) {
        this.request = request;
        this.serviceMethod = serviceMethod;
        this.svcConfigure = applicationContext.getBean(GatewayServiceConfigure.class);
        this.exceptionHandler = applicationContext.getBean(GsvcExceptionHandler.class);
        this.objectMapper = ResponseUtils.OBJECT_MAPPER;

    }

    public static ServerResponse handle(ServerRequest request, String caller, ServiceMethod serviceMethod,
            ApplicationContext applicationContext) {

        if (caller == null) {
            caller = request.headers().firstHeader("X-Gsvc-Caller");
        }

        return new ServiceMethodHandler(request, serviceMethod, applicationContext).run(caller);
    }

    private ServerResponse run(String caller) {
        try {
            if (!StringUtils.hasText(caller)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            logId = GsvcContextHolder.getRequestId();
            val type = serviceMethod.getType();
            if (log.isTraceEnabled()) {
                log.trace("[{}] Start to call method[{}]: {}@{}/{}", logId, type, serviceMethod.getServiceName(),
                        serviceMethod.getAppName(), serviceMethod.getDmName());
            }
            // 1. 解析请求体
            Object requestObj = deserializeRequest(type);

            // 2. 调用方法
            return switch (type) {
                case UNARY -> doUnaryCall(requestObj);
                case SERVER_STREAMING, BIDI_STREAMING -> doStreamingCall(requestObj);
                default -> exceptionHandler.handle(new ResponseStatusException(HttpStatus.BAD_REQUEST), request);
            };
        }
        catch (Exception e) {
            if (e instanceof ResponseStatusException || e instanceof HttpStatusCodeException) {
                log.warn("[{}] Call method failed: {}@{}/{} - {}", logId, serviceMethod.getServiceName(),
                        serviceMethod.getAppName(), serviceMethod.getDmName(), e.getMessage());
            }
            else {
                log.error("[{}] Call method failed: {}@{}/{}", logId, serviceMethod.getServiceName(),
                        serviceMethod.getAppName(), serviceMethod.getDmName(), e);
            }

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

    protected Object deserializeRequest(MethodDescriptor.MethodType type) throws IOException, ServletException {
        val contentType = request.headers().contentType().orElse(MediaType.APPLICATION_FORM_URLENCODED);

        Object requestObj;
        Mono<Object> args;

        val svcName = serviceMethod.getAppName();
        val reqClass = serviceMethod.reqClass();
        val dmName = serviceMethod.getDmName();
        val readTimeout = svcConfigure.getReadTimeout(svcName, dmName);

        if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            if (type == BIDI_STREAMING) {
                return Mono.create(sink -> {
                    try {
                        val requestBody = retrieveRequestBody(request.servletRequest());
                        log.trace("[{}] Request({}) resolved: {}", logId, contentType, requestBody);
                        sink.success(objectMapper.readValue(requestBody, reqClass));
                    }
                    catch (JsonProcessingException e) {
                        sink.error(e);
                    }
                });
            }
            else {
                val requestBody = retrieveRequestBody(request.servletRequest());
                log.trace("[{}] Request({}) resolved: {}", logId, contentType, requestBody);

                return objectMapper.readValue(requestBody, reqClass);
            }
        }
        else if (contentType.isCompatibleWith(MediaType.MULTIPART_FORM_DATA)
                || contentType.isCompatibleWith(MediaType.MULTIPART_MIXED)) {
            val queryParams = request.params();
            val servletRequest = request.servletRequest();

            if (servletRequest instanceof StandardMultipartHttpServletRequest standardMultipartHttpServletRequest) {
                val multiFileMap = standardMultipartHttpServletRequest.getMultiFileMap();
                val multipartData = Mono.just(multiFileMap);
                args = Mono.zip(Mono.just(queryParams), multipartData).map(tuple -> {
                    Map<String, Object> result = new HashMap<>();
                    tuple.getT1().forEach((key, values) -> addBindValue(result, key, values));
                    tuple.getT2().forEach((key, values) -> addBindValue(result, key, values));
                    return result;
                });
            }
            else {
                args = Mono.error(new ResponseStatusException(HttpStatus.NO_CONTENT));
            }
        }
        else {
            MultiValueMap<String, String> queryParams = request.params();
            args = Mono.just(queryParams).map(params -> {
                Map<String, Object> result = new HashMap<>();
                for (Map.Entry<String, List<String>> kv : params.entrySet()) {
                    addBindValue(result, kv.getKey(), kv.getValue());
                }
                return result;
            });
        }
        if (type == BIDI_STREAMING) {
            return args.handle((arg, sink) -> {
                try {
                    val reqObj = objectMapper.readValue(objectMapper.writeValueAsBytes(arg), reqClass);
                    if (log.isTraceEnabled()) {
                        log.trace("[{}] Request({}) resolved: {}", logId, contentType,
                                objectMapper.writeValueAsString(arg));
                    }
                    sink.next(reqObj);
                    sink.complete();
                }
                catch (IOException e) {
                    log.error("[{}] Request({}) resolved failed: {}", logId, contentType, e.getMessage());
                    sink.error(e);
                }
            }).timeout(readTimeout);
        }
        else {
            val reqObj = args.blockOptional(readTimeout);

            requestObj = objectMapper.readValue(objectMapper.writeValueAsBytes(reqObj.orElseThrow()), reqClass);
            if (log.isTraceEnabled()) {
                log.trace("[{}] {} resolved: {}", logId, contentType, objectMapper.writeValueAsString(requestObj));
            }
        }

        return requestObj;
    }

    private void addBindValue(Map<String, Object> params, String key, List<?> values) {
        if (params.containsKey(key)) {
            return;
        }

        if (!CollectionUtils.isEmpty(values)) {
            List<?> args = values.stream().map(value -> {
                if (value instanceof MultipartFile filePart) {
                    val file = UploadFile.builder()
                        .name(filePart.getName())
                        .ext(FileUtil.extName(filePart.getOriginalFilename()))
                        .filename(filePart.getOriginalFilename())
                        .contentType(Optional.ofNullable(filePart.getContentType()).orElse(MediaType.TEXT_PLAIN_VALUE));
                    try {
                        val tmpFile = File.createTempFile("UP_LD_", ".part");
                        file.file(tmpFile.getAbsolutePath()).size(filePart.getSize());
                        filePart.transferTo(tmpFile);

                        if (log.isTraceEnabled()) {
                            log.trace("[{}] 文件'{}'已上传到: {}", logId, filePart.getOriginalFilename(),
                                    tmpFile.getAbsoluteFile());
                        }

                        return file.build();
                    }
                    catch (IOException e) {
                        log.error("[{}] Upload file '{}' error: {}", logId, filePart.getOriginalFilename(),
                                e.getMessage());
                        return file.size(-1).error(e.getMessage()).build();
                    }
                }
                else if (value instanceof FormFieldPart formFieldPart) {
                    return formFieldPart.value();
                }
                else {
                    return value;
                }
            }).toList();

            params.put(key, args.size() == 1 ? args.get(0) : args);
        }
    }

    private String createResponse(Object resp) throws JsonProcessingException {
        val respStr = objectMapper.writeValueAsString(resp);
        if (log.isTraceEnabled()) {
            log.trace("[{}] Response of {}@{}/{} is: {}", logId, serviceMethod.getServiceName(),
                    serviceMethod.getAppName(), serviceMethod.getDmName(), StringUtils.truncate(respStr, 256));
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
