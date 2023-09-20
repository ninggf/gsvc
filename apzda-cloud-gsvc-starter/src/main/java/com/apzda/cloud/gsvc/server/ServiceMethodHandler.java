package com.apzda.cloud.gsvc.server;

import cn.hutool.core.io.FileUtil;
import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.dto.UploadFile;
import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import com.apzda.cloud.gsvc.plugin.IPlugin;
import com.apzda.cloud.gsvc.plugin.IPostInvoke;
import com.apzda.cloud.gsvc.plugin.IPreInvoke;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    private String caller;

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

        if (!StringUtils.hasText(caller)) {
            caller = request.headers().firstHeader("X-Gsvc-Caller");
        }

        return new ServiceMethodHandler(request, serviceMethod, applicationContext).run(caller);
    }

    private ServerResponse run(String caller) {
        try {
            if (!StringUtils.hasText(caller)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            this.caller = caller;
            logId = GsvcContextHolder.getRequestId();
            val type = serviceMethod.getType();
            if (log.isTraceEnabled()) {
                log.trace("[{}] Start to call method[{}]: {}.{}", logId, type, serviceMethod.getServiceName(),
                        serviceMethod.getDmName());
            }
            // 1. 解析请求体
            Mono<JsonNode> requestObj = deserializeRequest(type);

            // 2. 调用方法
            return switch (type) {
                case UNARY -> doUnaryCall(requestObj);
                case SERVER_STREAMING -> doStreamingCall(requestObj);
                default -> exceptionHandler.handle(new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED), request);
            };
        }
        catch (Throwable throwable) {
            Throwable e = throwable;

            if (throwable instanceof InvocationTargetException) {
                e = ((InvocationTargetException) throwable).getTargetException();
            }

            log.error("[{}] Call method failed: {}.{}", logId, serviceMethod.getServiceName(),
                    serviceMethod.getDmName(), e);
            // bookmark exception handle(service call)
            return exceptionHandler.handle(e, request);
        }
    }

    @SuppressWarnings("unchecked")
    private ServerResponse doStreamingCall(Mono<JsonNode> requestObj)
            throws InvocationTargetException, IllegalAccessException {
        val plugins = serviceMethod.getPlugins();
        var size = plugins.size();

        for (IPlugin plugin : plugins) {
            if (plugin instanceof IPreInvoke preInvoke) {
                requestObj = preInvoke.preInvoke(request, requestObj, serviceMethod);
            }
        }

        Mono<Object> realReqObj = requestObj.handle((req, sink) -> {
            try {
                sink.next(objectMapper.readValue(req.toString(), serviceMethod.getRequestType()));
                sink.complete();
            }
            catch (JsonProcessingException e) {
                sink.error(e);
            }
        });

        Flux<Object> resultObj = (Flux<Object>) serviceMethod.call(realReqObj.block());

        while (--size >= 0) {
            var plugin = plugins.get(size);
            if (plugin instanceof IPostInvoke preInvoke) {
                resultObj = (Flux<Object>) preInvoke.postInvoke(requestObj, resultObj, serviceMethod);
            }
        }

        val timeout = svcConfigure.getTimeout(serviceMethod.getCfgName(), serviceMethod.getDmName());
        if (!timeout.isZero()) {
            resultObj = resultObj.timeout(timeout);
        }

        final Flux<Object> responseFlux = resultObj.contextCapture();
        // reactive is so hard!!!
        return ServerResponse.sse(sseBuilder -> {
            responseFlux.doOnComplete(sseBuilder::complete).doOnError(err -> {
                log.error("[{}] Call method failed: {}.{}", logId, serviceMethod.getServiceName(),
                        serviceMethod.getDmName(), err);
                try {
                    sseBuilder.data(ResponseUtils.fallback(err, serviceMethod.getServiceName(), String.class));
                    sseBuilder.complete();
                }
                catch (IOException ie) {
                    sseBuilder.error(ie);
                }
            }).subscribe(resp -> {
                try {
                    val response = createResponse(resp);
                    log.warn("序列化响应: {}", response);
                    sseBuilder.data(response);
                }
                catch (IOException e) {
                    log.error("[{}] Call method failed: {}.{}", logId, serviceMethod.getServiceName(),
                            serviceMethod.getDmName(), e);
                    try {
                        sseBuilder.data(ResponseUtils.fallback(e, serviceMethod.getServiceName(), String.class));
                    }
                    catch (IOException ie) {
                        sseBuilder.error(ie);
                    }
                }
            });
        });
    }

    private ServerResponse doUnaryCall(Mono<JsonNode> requestObj)
            throws InvocationTargetException, IllegalAccessException, JsonProcessingException {
        val plugins = serviceMethod.getPlugins();
        var size = plugins.size();
        for (IPlugin plugin : plugins) {
            if (plugin instanceof IPreInvoke preInvoke) {
                requestObj = preInvoke.preInvoke(request, requestObj, serviceMethod);
            }
        }
        //
        Object realReqObj = requestObj.handle((req, sink) -> {
            try {
                sink.next(objectMapper.readValue(req.toString(), serviceMethod.getRequestType()));
                sink.complete();
            }
            catch (JsonProcessingException e) {
                sink.error(e);
            }
        }).block();

        Object returnObj = serviceMethod.call(realReqObj);

        while (--size >= 0) {
            var plugin = plugins.get(size);
            if (plugin instanceof IPostInvoke preInvoke) {
                returnObj = preInvoke.postInvoke(requestObj, returnObj, serviceMethod);
            }
        }

        val response = createResponse(returnObj);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(response);
    }

    protected Mono<JsonNode> deserializeRequest(MethodDescriptor.MethodType type) throws IOException {
        val contentType = request.headers().contentType().orElse(MediaType.APPLICATION_FORM_URLENCODED);

        Mono<Object> args;

        val cfgName = serviceMethod.getCfgName();
        val reqClass = serviceMethod.reqClass();
        val dmName = serviceMethod.getDmName();
        val readTimeout = svcConfigure.getReadTimeout(cfgName, false);

        if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            return Mono.<JsonNode>create(sink -> {
                try {
                    val requestBody = objectMapper.readTree(request.servletRequest().getReader());
                    // val requestBody =
                    // retrieveRequestBody(request.servletRequest());
                    if (log.isTraceEnabled()) {
                        log.trace("[{}] Request({}) resolved: {}", logId, contentType, requestBody);
                    }
                    sink.success(requestBody);
                }
                catch (IOException e) {
                    sink.error(e);
                }
            }).timeout(readTimeout);
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

        return args.<JsonNode>handle((arg, sink) -> {
            try {
                val reqObj = objectMapper.convertValue(arg, JsonNode.class);
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

        if ("gtw".equals(this.caller)) {
            // bookmark: wrap response for the request from gateway.
            val node = objectMapper.convertValue(resp, JsonNode.class);
            if (node instanceof ObjectNode objectNode) {
                val wrappedResp = new Response<JsonNode>();
                val errCode = objectNode.get("errCode").asInt();
                objectNode.remove("errCode");
                wrappedResp.setErrCode(errCode);
                if (objectNode.has("errMsg")) {
                    val errMsg = objectNode.get("errMsg").asText();
                    objectNode.remove("errMsg");
                    wrappedResp.setErrMsg(errMsg);
                }
                wrappedResp.setData(node);
                resp = wrappedResp;
            }
        }

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
