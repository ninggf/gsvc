package com.apzda.cloud.gsvc.server;

import cn.hutool.core.io.FileUtil;
import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.dto.UploadFile;
import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import com.apzda.cloud.gsvc.plugin.IPlugin;
import com.apzda.cloud.gsvc.plugin.IPostInvoke;
import com.apzda.cloud.gsvc.plugin.IPreInvoke;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.MethodDescriptor;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                log.trace("[{}] Start to call method[{}]: {}.{}", logId, type, serviceMethod.getServiceName(),
                        serviceMethod.getDmName());
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
        catch (Throwable throwable) {
            Throwable e = throwable;

            if (throwable instanceof InvocationTargetException) {
                e = ((InvocationTargetException) throwable).getTargetException();
            }

            log.error("[{}] Call method failed: {}.{}", logId, serviceMethod.getServiceName(),
                    serviceMethod.getDmName(), e);

            return exceptionHandler.handle(e, request);
        }
    }

    @SuppressWarnings("unchecked")
    private ServerResponse doStreamingCall(Object requestObj) throws InvocationTargetException, IllegalAccessException {
        val plugins = serviceMethod.getPlugins();
        var size = plugins.size();
        for (IPlugin plugin : plugins) {
            if (plugin instanceof IPreInvoke preInvoke) {
                requestObj = preInvoke.preInvoke(request, requestObj, serviceMethod);
            }
        }

        Mono<Object> mono = (Mono<Object>) serviceMethod.call(requestObj);
        while (--size >= 0) {
            var plugin = plugins.get(size);
            if (plugin instanceof IPostInvoke preInvoke) {
                mono = (Mono<Object>) preInvoke.postInvoke(request, requestObj, mono, serviceMethod);
            }
        }
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(mono);
    }

    private ServerResponse doUnaryCall(Object requestObj)
            throws InvocationTargetException, IllegalAccessException, JsonProcessingException {
        val plugins = serviceMethod.getPlugins();
        var size = plugins.size();
        for (IPlugin plugin : plugins) {
            if (plugin instanceof IPreInvoke preInvoke) {
                requestObj = preInvoke.preInvoke(request, requestObj, serviceMethod);
            }
        }
        Object resp = serviceMethod.call(requestObj);
        while (--size >= 0) {
            var plugin = plugins.get(size);
            if (plugin instanceof IPostInvoke preInvoke) {
                resp = preInvoke.postInvoke(request, requestObj, resp, serviceMethod);
            }
        }
        val response = createResponse(resp);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(response);
    }

    protected Object deserializeRequest(MethodDescriptor.MethodType type) throws IOException {
        val contentType = request.headers().contentType().orElse(MediaType.APPLICATION_FORM_URLENCODED);

        Object requestObj;
        Mono<Object> args;

        val cfgName = serviceMethod.getCfgName();
        val reqClass = serviceMethod.reqClass();
        val dmName = serviceMethod.getDmName();
        val readTimeout = svcConfigure.getReadTimeout(cfgName, dmName, false);

        if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            if (type == BIDI_STREAMING) {
                return Mono.create(sink -> {
                    try {
                        val requestBody = retrieveRequestBody(request.servletRequest());
                        log.trace("[{}] Request({}) resolved: {}", logId, contentType, requestBody);
                        sink.success(objectMapper.readValue(requestBody, reqClass));
                    }
                    catch (IOException e) {
                        sink.error(e);
                    }
                }).timeout(readTimeout);
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
                log.trace("[{}] Request({}) resolved: {}", logId, contentType,
                        objectMapper.writeValueAsString(requestObj));
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
                            log.trace("[{}] File '{}' uploaded to '{}'", logId, filePart.getOriginalFilename(),
                                    tmpFile.getAbsoluteFile());
                        }

                        return file.build();
                    }
                    catch (IOException e) {
                        log.error("[{}] Upload file '{}' failed: {}", logId, filePart.getOriginalFilename(),
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
            log.trace("[{}] Response of {}.{}: {}", logId, serviceMethod.getServiceName(), serviceMethod.getDmName(),
                    respStr);
        }
        return respStr;
    }

    private String retrieveRequestBody(HttpServletRequest request) throws IOException {
        val req = new ContentCachingRequestWrapper(request);
        try (val reader = req.getReader()) {
            val stringBuilder = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                stringBuilder.append(line);
                line = reader.readLine();
            }
            return stringBuilder.toString();
        }
        catch (IOException e) {
            log.error("[{}] Read Request body failed: {}", logId, e.getMessage());
            throw e;
        }
    }

}
