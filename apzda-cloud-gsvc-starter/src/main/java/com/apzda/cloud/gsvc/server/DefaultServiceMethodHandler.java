package com.apzda.cloud.gsvc.server;

import build.buf.protovalidate.Validator;
import build.buf.protovalidate.exceptions.ValidationException;
import cn.hutool.core.io.FileUtil;
import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.converter.EncryptedMessageConverter;
import com.apzda.cloud.gsvc.core.GatewayServiceRegistry;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.dto.MessageType;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.dto.UploadFile;
import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import com.apzda.cloud.gsvc.exception.MessageValidationException;
import com.apzda.cloud.gsvc.plugin.IPlugin;
import com.apzda.cloud.gsvc.plugin.IPostInvoke;
import com.apzda.cloud.gsvc.plugin.IPreInvoke;
import com.apzda.cloud.gsvc.utils.I18nUtils;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.protobuf.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.WebUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
public class DefaultServiceMethodHandler implements IServiceMethodHandler {

    private final GatewayServiceConfigure svcConfigure;

    private final ObjectMapper objectMapper;

    private final GsvcExceptionHandler exceptionHandler;

    private final Validator validator;

    private final MultipartResolver multipartResolver;

    private final EncryptedMessageConverter encryptedMessageConverter;

    @Override
    public ServerResponse handleUnary(ServerRequest request, Class<?> serviceClz, String method,
            Function<Object, Object> func) {
        try {
            val serviceMethod = GatewayServiceRegistry.getDeclaredServiceMethods(serviceClz).get(method);
            if (serviceMethod == null) {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
            }
            val caller = func == null ? GTW : request.headers().firstHeader(IServiceMethodHandler.CALLER_HEADER);
            val requestObj = createRequestObj(request, serviceMethod, caller);
            return doUnaryCall(request, requestObj, serviceMethod, func);
        }
        catch (Throwable throwable) {
            if (throwable instanceof ReflectiveOperationException && throwable.getCause() != null) {
                return exceptionHandler.handle(throwable.getCause(), request);
            }
            return exceptionHandler.handle(throwable, request);
        }
    }

    @Override
    public ServerResponse handleServerStreaming(ServerRequest request, Class<?> serviceClz, String method,
            Function<Object, Object> func) {
        try {
            val serviceMethod = GatewayServiceRegistry.getDeclaredServiceMethods(serviceClz).get(method);
            if (serviceMethod == null) {
                throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
            }
            val caller = func == null ? GTW : request.headers().firstHeader(IServiceMethodHandler.CALLER_HEADER);
            val requestObj = createRequestObj(request, serviceMethod, caller);
            return doStreamingCall(request, requestObj, serviceMethod, func);
        }
        catch (Throwable throwable) {
            if (throwable instanceof ReflectiveOperationException && throwable.getCause() != null) {
                return exceptionHandler.handle(throwable.getCause(), request);
            }
            return exceptionHandler.handle(throwable, request);
        }
    }

    @Override
    public ServerResponse handleBidStreaming(ServerRequest request, Class<?> serviceClz, String method,
            Function<Object, Object> func) {
        return ServerResponse.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    protected Mono<JsonNode> createRequestObj(ServerRequest request, ServiceMethod serviceMethod, String caller) {
        if (!StringUtils.hasText(caller)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        val context = GsvcContextHolder.getContext();
        context.setCaller(caller);

        if (log.isTraceEnabled()) {
            val type = serviceMethod.getType();
            log.trace("Call method[{}]({}.{}) from {}", type, serviceMethod.getServiceName(), serviceMethod.getDmName(),
                    caller);
        }
        // 1. 解析请求体
        return deserializeRequest(request, serviceMethod);
    }

    @SuppressWarnings("unchecked")
    private ServerResponse doStreamingCall(ServerRequest request, Mono<JsonNode> requestObj,
            ServiceMethod serviceMethod, Function<Object, Object> func)
            throws ReflectiveOperationException, ValidationException {
        val plugins = serviceMethod.getPlugins();
        var size = plugins.size();

        for (IPlugin plugin : plugins) {
            if (plugin instanceof IPreInvoke preInvoke) {
                requestObj = preInvoke.preInvoke(request, requestObj, serviceMethod);
            }
        }

        Object realReqObj = requestObj.handle((req, sink) -> {
            try {
                val reqObj = objectMapper.readValue(req.toString(), serviceMethod.getRequestType());
                val result = validator.validate((Message) reqObj);
                // Check if there are any validation violations
                if (!result.isSuccess()) {
                    sink.error(new MessageValidationException(result.getViolations(),
                            ((Message) reqObj).getDescriptorForType()));
                    return;
                }
                sink.next(reqObj);
                sink.complete();
            }
            catch (Exception e) {
                sink.error(e);
            }
        }).block();

        assert realReqObj != null;
        val result = validator.validate((Message) realReqObj);
        // Check if there are any validation violations
        if (!result.isSuccess()) {
            throw new MessageValidationException(result.getViolations(), ((Message) realReqObj).getDescriptorForType());
        }

        Flux<Object> returnObj;
        if (func == null) {
            returnObj = (Flux<Object>) serviceMethod.call(realReqObj);
        }
        else {
            returnObj = (Flux<Object>) func.apply(realReqObj);
        }

        val timeout = svcConfigure.getTimeout(serviceMethod.getCfgName(), serviceMethod.getDmName());
        if (timeout.toMillis() > 0) {
            returnObj = returnObj.timeout(timeout);
        }

        while (--size >= 0) {
            var plugin = plugins.get(size);
            if (plugin instanceof IPostInvoke preInvoke) {
                returnObj = (Flux<Object>) preInvoke.postInvoke(requestObj, returnObj, serviceMethod);
            }
        }

        final Flux<Object> responseFlux = returnObj.contextCapture();
        val context = GsvcContextHolder.getContext();
        // server-streaming method will respond text/event-stream
        return ServerResponse.sse(sseBuilder -> {
            responseFlux.doOnComplete(sseBuilder::complete).doOnError(err -> {
                context.restore();
                log.error("Call method failed: {}.{} - {}", serviceMethod.getServiceName(), serviceMethod.getDmName(),
                        err.getMessage());
                try {
                    sseBuilder.data(ResponseUtils.fallback(err, serviceMethod.getServiceName(), String.class));
                    sseBuilder.complete();
                }
                catch (IOException ie) {
                    log.warn("Cannot send data to client: {}.{} - {}", serviceMethod.getServiceName(),
                            serviceMethod.getDmName(), ie.getMessage());
                    sseBuilder.complete();
                }
            }).subscribe(resp -> {
                context.restore();
                try {
                    val response = createResponse(request, serviceMethod, resp);
                    sseBuilder.data(response);
                }
                catch (Exception e) {
                    log.error("Call method failed on subscribe: {}.{} - {}", serviceMethod.getServiceName(),
                            serviceMethod.getDmName(), e.getMessage());
                    try {
                        sseBuilder.data(ResponseUtils.fallback(e, serviceMethod.getServiceName(), String.class));
                    }
                    catch (IOException ie) {
                        log.error("Cannot send data to client: {}.{} - {}", serviceMethod.getServiceName(),
                                serviceMethod.getDmName(), ie.getMessage());
                        sseBuilder.complete();
                    }
                }
            });
        });
    }

    private ServerResponse doUnaryCall(ServerRequest request, Mono<JsonNode> requestObj, ServiceMethod serviceMethod,
            Function<Object, Object> func)
            throws JsonProcessingException, ValidationException, ReflectiveOperationException {
        val plugins = serviceMethod.getPlugins();
        var size = plugins.size();
        for (IPlugin plugin : plugins) {
            if (plugin instanceof IPreInvoke preInvoke) {
                requestObj = preInvoke.preInvoke(request, requestObj, serviceMethod);
            }
        }
        val requestType = serviceMethod.getRequestType();

        Object realReqObj = requestObj.handle((req, sink) -> {
            try {
                sink.next(objectMapper.readValue(req.toString(), requestType));
                sink.complete();
            }
            catch (JsonProcessingException e) {
                sink.error(e);
            }
        }).block();

        assert realReqObj != null;
        val result = validator.validate((Message) realReqObj);
        // Check if there are any validation violations
        if (!result.isSuccess()) {
            throw new MessageValidationException(result.getViolations(), ((Message) realReqObj).getDescriptorForType());
        }
        Object returnObj;
        if (func == null) {
            returnObj = serviceMethod.call(realReqObj);
        }
        else {
            returnObj = func.apply(realReqObj);
        }

        while (--size >= 0) {
            var plugin = plugins.get(size);
            if (plugin instanceof IPostInvoke preInvoke) {
                returnObj = preInvoke.postInvoke(requestObj, returnObj, serviceMethod);
            }
        }
        try {
            val response = createResponse(request, serviceMethod, returnObj);
            return createServerResponse(request, serviceMethod, response);
        }
        catch (IOException e) {
            log.error("Cannot create response", e);
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @SuppressWarnings("all")
    protected Mono<JsonNode> deserializeRequest(ServerRequest request, ServiceMethod serviceMethod) {
        val contentType = request.headers().contentType().orElse(MediaType.APPLICATION_FORM_URLENCODED);

        Mono<Object> args;

        // bookmark: readTimeout
        val readTimeout = svcConfigure.getReadTimeout(serviceMethod, false);
        val context = GsvcContextHolder.getContext();
        val httpServletRequest = request.servletRequest();

        if (encryptedMessageConverter.canRead(serviceMethod.getRequestType(), contentType)) {
            var mono = Mono.<JsonNode>create(sink -> {
                try {
                    val obj = encryptedMessageConverter.read(serviceMethod.getRequestType(),
                            new ServletServerHttpRequest(httpServletRequest));
                    val requestBody = objectMapper.convertValue(obj, JsonNode.class);
                    if (log.isTraceEnabled()) {
                        context.restore();
                        log.trace("Request({}) resolved: {}", contentType, requestBody);
                    }
                    sink.success(requestBody);
                }
                catch (IOException e) {
                    sink.error(e);
                }
            });

            if (readTimeout.toMillis() > 0) {
                mono = mono.timeout(readTimeout);
            }
            return mono;
        }
        else if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            var mono = Mono.<JsonNode>create(sink -> {
                try {
                    JsonNode requestBody;
                    if (httpServletRequest.getContentLength() == 0) {
                        requestBody = objectMapper.readTree("{}");
                    }
                    else {
                        requestBody = objectMapper.readTree(httpServletRequest.getReader());
                    }

                    if (log.isTraceEnabled()) {
                        context.restore();
                        log.trace("Request({}) resolved: {}", contentType, requestBody);
                    }
                    sink.success(requestBody);
                }
                catch (IOException e) {
                    sink.error(e);
                }
            });
            if (readTimeout.toMillis() > 0) {
                mono = mono.timeout(readTimeout);
            }
            return mono;
        }
        else if (multipartResolver.isMultipart(httpServletRequest)) {
            MultipartHttpServletRequest multipartRequest = WebUtils.getNativeRequest(httpServletRequest,
                    MultipartHttpServletRequest.class);
            if (multipartRequest == null) {
                multipartRequest = new StandardMultipartHttpServletRequest(httpServletRequest);
            }
            val queryParams = request.params();
            val multiFileMap = multipartRequest.getMultiFileMap();
            val multipartData = Mono.just(multiFileMap);
            args = Mono.zip(Mono.just(queryParams), multipartData).map(tuple -> {
                Map<String, Object> result = new HashMap<>();
                tuple.getT1().forEach((key, values) -> addBindValue(result, key, values));
                tuple.getT2().forEach((key, values) -> addBindValue(result, key, values));
                return result;
            });
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

        var mono = args.<JsonNode>handle((arg, sink) -> {
            try {
                context.restore();
                val reqObj = objectMapper.convertValue(arg, JsonNode.class);
                if (log.isTraceEnabled()) {
                    log.trace("Request({}) resolved: {}", contentType, objectMapper.writeValueAsString(arg));
                }
                sink.next(reqObj);
                sink.complete();
            }
            catch (IOException e) {
                log.error("Request({}) resolved failed: {}", contentType, e.getMessage());
                sink.error(e);
            }
        });

        if (readTimeout.toMillis() > 0) {
            mono = mono.timeout(readTimeout);
        }
        return mono;
    }

    private void addBindValue(Map<String, Object> params, String key, List<?> values) {
        if (params.containsKey(key)) {
            return;
        }
        val context = GsvcContextHolder.getContext();
        if (!CollectionUtils.isEmpty(values)) {
            List<?> args = values.stream().map(value -> {
                context.restore();
                if (value instanceof MultipartFile filePart) {
                    val originalFilename = filePart.getOriginalFilename();
                    val file = UploadFile.builder()
                        .name(filePart.getName())
                        .ext(FileUtil.extName(originalFilename))
                        .filename(originalFilename)
                        .contentType(Optional.ofNullable(filePart.getContentType()).orElse(MediaType.TEXT_PLAIN_VALUE));
                    try {
                        val tmpFile = File.createTempFile("UP_LD_", ".part");
                        file.file(tmpFile.getAbsolutePath()).size(filePart.getSize());
                        filePart.transferTo(tmpFile);

                        if (log.isTraceEnabled()) {
                            log.trace("File '{}' uploaded to '{}'", originalFilename, tmpFile.getAbsoluteFile());
                        }

                        return file.build();
                    }
                    catch (IOException e) {
                        log.error("Upload file '{}' failed: {}", originalFilename, e.getMessage());
                        return file.size(-1).error(e.getMessage()).build();
                    }
                }
                else if (value instanceof FormFieldPart formFieldPart) {
                    val name = formFieldPart.name();
                    val content = formFieldPart.value();
                    val headers = formFieldPart.headers();
                    headers.getContentType();
                    val file = UploadFile.builder()
                        .name("file")
                        .ext(FileUtil.extName(name))
                        .filename(name)
                        .contentType(Optional.ofNullable(headers.getContentType())
                            .orElse(MediaType.TEXT_PLAIN)
                            .removeQualityValue()
                            .toString());
                    try {
                        val tmpFile = File.createTempFile("UP_LD_", ".part");
                        file.file(tmpFile.getAbsolutePath()).size(content.length());
                        try (val writer = new FileWriter(tmpFile)) {
                            FileCopyUtils.copy(content, writer);
                        }

                        if (log.isTraceEnabled()) {
                            log.trace("File '{}' uploaded to '{}'", name, tmpFile.getAbsoluteFile());
                        }

                        return file.build();
                    }
                    catch (IOException e) {
                        log.error("Upload file '{}' failed: {}", name, e.getMessage());
                        return file.size(-1).error(e.getMessage()).build();
                    }
                }
                else {
                    return value;
                }
            }).toList();

            params.put(key, args.size() == 1 ? args.get(0) : args);
        }
    }

    private String createResponse(ServerRequest request, ServiceMethod serviceMethod, Object resp) throws IOException {
        val context = GsvcContextHolder.getContext();
        val flat = svcConfigure.isFlatResponse();
        val node = objectMapper.convertValue(resp, JsonNode.class);
        if (!flat && GTW.equals(context.getCaller())) {
            // bookmark: wrap response for the request from gateway.
            if (node instanceof ObjectNode objectNode) {
                val wrappedResp = new Response<JsonNode>();
                val errCode = Optional.ofNullable(objectNode.remove("errCode")).orElse(new IntNode(0)).asInt();
                wrappedResp.setErrCode(errCode);
                if (objectNode.has("errMsg")) {
                    val errMsg = objectNode.remove("errMsg").asText();
                    if (StringUtils.hasText(errMsg)) {
                        wrappedResp.setErrMsg(errMsg);
                    }
                }
                else if (errCode != 0) {
                    val errMsg = I18nUtils.t("error." + Math.abs(errCode), "");
                    if (StringUtils.hasText(errMsg)) {
                        wrappedResp.setErrMsg(errMsg);
                    }
                }

                val errType = objectNode.remove("errType");
                if (errType != null) {
                    wrappedResp.setType(MessageType.fromString(errType.asText()));
                }

                wrappedResp.setData(objectNode);
                resp = wrappedResp;
            }
        }
        else if (node instanceof ObjectNode objectNode) {
            val errCode = Optional.ofNullable(objectNode.get("errCode")).orElse(new IntNode(0)).asInt();
            objectNode.set("errCode", new IntNode(errCode));
            if (!objectNode.has("errMsg")) {
                val errMsg = I18nUtils.t("error." + Math.abs(errCode), "");
                if (StringUtils.hasText(errMsg)) {
                    objectNode.set("errMsg", new TextNode(errMsg));
                }
            }
            resp = objectNode;
        }

        val respStr = objectMapper.writeValueAsString(resp);
        if (log.isTraceEnabled()) {
            log.trace("Response of {}.{}: {}", serviceMethod.getServiceName(), serviceMethod.getDmName(), respStr);
        }

        val returnType = serviceMethod.getReturnType();
        val headers = request.headers();
        val accepts = headers.accept();
        if (accepts.stream().anyMatch(mediaType -> encryptedMessageConverter.canWrite(returnType, mediaType))) {
            return encryptedMessageConverter.encrypt(headers.asHttpHeaders(), respStr);
        }

        return respStr;
    }

    private ServerResponse createServerResponse(ServerRequest request, ServiceMethod serviceMethod, String response) {
        val returnType = serviceMethod.getReturnType();
        val accept = request.headers()
            .accept()
            .stream()
            .filter(mediaType -> encryptedMessageConverter.canWrite(returnType, mediaType))
            .findFirst();

        return accept.map(mediaType -> ServerResponse.ok().contentType(mediaType).body(response))
            .orElseGet(() -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(response));
    }

}
