package com.apzda.cloud.gsvc.client;

import build.buf.protovalidate.Validator;
import build.buf.protovalidate.exceptions.ValidationException;
import cn.hutool.core.net.URLEncodeUtil;
import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.core.GatewayServiceRegistry;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.core.ServiceMethod;
import com.apzda.cloud.gsvc.exception.MessageValidationException;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.gsvc.plugin.IPlugin;
import com.apzda.cloud.gsvc.plugin.IPostCall;
import com.apzda.cloud.gsvc.plugin.IPreCall;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClientRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author fengz
 */
@Slf4j
public class DefaultRemoteServiceCaller implements IServiceCaller {

    public static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
    };

    protected final ObjectMapper objectMapper = ResponseUtils.OBJECT_MAPPER;

    protected final ApplicationContext applicationContext;

    protected final GatewayServiceConfigure svcConfigure;

    protected final Validator validator;

    public DefaultRemoteServiceCaller(ApplicationContext applicationContext, GatewayServiceConfigure svcConfigure) {
        this.applicationContext = applicationContext;
        this.svcConfigure = svcConfigure;
        this.validator = applicationContext.getBean(Validator.class);
        // bookmark:
        // https://github.com/reactive-streams/reactive-streams-jvm/blob/master/README.md#1.7
        Hooks.onErrorDropped(error -> {
            if (log.isTraceEnabled()) {
                val context = GsvcContextHolder.getContext();
                log.trace("Error dropped while doing RPC({}): {}", context.getSvcName(), error.getMessage());
            }
        });
    }

    @Override
    public <T, R> R unaryCall(Class<?> clazz, String method, T request, Class<T> reqClazz, Class<R> resClazz) {
        val serviceMethod = GatewayServiceRegistry.getServiceMethod(clazz, method);
        val url = serviceMethod.getRpcAddr();
        if (log.isDebugEnabled()) {
            log.debug("Start Block RPC: {}", url);
        }
        val reqBody = prepareRequest(request, serviceMethod);
        return doBlockCall(reqBody.bodyToMono(String.class), serviceMethod, url, resClazz);
    }

    protected <R> R doBlockCall(Mono<String> reqBody, ServiceMethod serviceMethod, String uri, Class<R> rClass) {
        // bookmark: block rpc
        val res = handleRpcFallback(Flux.concat(reqBody), serviceMethod, String.class).blockFirst();

        if (log.isDebugEnabled()) {
            log.debug("Response from {}: {}", uri, res);
        }

        return ResponseUtils.parseResponse(res, rClass);
    }

    @Override
    public <T, R> Flux<R> serverStreamingCall(Class<?> clazz, String method, T request, Class<T> reqClazz,
            Class<R> resClazz) {
        val serviceMethod = GatewayServiceRegistry.getServiceMethod(clazz, method);
        val url = serviceMethod.getRpcAddr();

        if (log.isTraceEnabled()) {
            log.trace("Start Async RPC: {}", url);
        }
        val reqBody = prepareRequest(request, serviceMethod);

        return doAsyncCall(reqBody.bodyToFlux(SSE_RESPONSE_TYPE), serviceMethod, url, resClazz);
    }

    protected <R> Flux<R> doAsyncCall(Flux<ServerSentEvent<String>> reqBody, ServiceMethod serviceMethod, String uri,
            Class<R> rClass) {
        // bookmark: async rpc
        val context = GsvcContextHolder.getContext();
        var reqMono = reqBody.map(res -> {
            context.restore();
            if (log.isTraceEnabled()) {
                log.trace("Response from {}: {}", uri, res);
            }
            try {
                val data = res.data();
                return ResponseUtils.parseResponse(data, rClass);
            }
            catch (Exception e) {
                log.trace("Cannot parse response from {}: {}", uri, res);
                return ResponseUtils.fallback(e, serviceMethod.getServiceName(), rClass);
            }
        });

        return handleRpcFallback(reqMono, serviceMethod, rClass);
    }

    protected <R> Flux<R> handleRpcFallback(Flux<R> reqBody, ServiceMethod method, Class<R> rClass) {
        // bookmark: fallback
        val serviceInfo = GatewayServiceRegistry.getServiceInfo(method.getInterfaceName());
        val uri = method.getRpcAddr();
        val plugins = method.getPlugins();
        var size = plugins.size();
        val context = GsvcContextHolder.getContext();
        reqBody = reqBody.doOnError(err -> {
            context.restore();
            log.error("RPC({}) failed: {}", uri, err.getMessage());
        });

        while (--size >= 0) {
            val plugin = plugins.get(size);
            if (plugin instanceof IPostCall postPlugin) {
                reqBody = postPlugin.postCall(serviceInfo, reqBody, method, rClass);
            }
        }

        return reqBody.contextCapture();
    }

    protected WebClient.ResponseSpec prepareRequest(Object requestObj, ServiceMethod method) {
        var context = GsvcContextHolder.getContext();
        context.setAttributes(RequestContextHolder.getRequestAttributes());
        context.setSvcName(method.getCfgName());

        val url = method.getRpcAddr();
        val readTimeout = svcConfigure.getReadTimeout(method, true);

        val webClient = applicationContext.getBean(method.getClientBeanName(), WebClient.class);
        WebClient.RequestBodySpec req = webClient.post().uri(url).accept(MediaType.APPLICATION_JSON);

        if (readTimeout.toMillis() > 0) {
            // method level read timeout
            req = req.httpRequest((httpRequest) -> {
                HttpClientRequest nr = httpRequest.getNativeRequest();
                nr.responseTimeout(readTimeout);
            });
        }

        List<IPlugin> plugins = method.getPlugins();

        for (IPlugin plugin : plugins) {
            if (plugin instanceof IPreCall prePlugin) {
                req = prePlugin.preCall(req, requestObj, method);
            }
        }

        try {
            val requestMsg = (Message) requestObj;
            val result = validator.validate(requestMsg);
            // Check if there are any validation violations
            if (!result.isSuccess()) {
                throw new MessageValidationException(result.getViolations(), requestMsg.getDescriptorForType());
            }
            return buildRequestBody(method, req, requestMsg).retrieve()
                .onStatus(
                        (status) -> status != HttpStatus.OK && (status.is2xxSuccessful() || status.is3xxRedirection()),
                        (response) -> {
                            context.restore();
                            val exception = new ErrorResponseException(response.statusCode());
                            exception.getHeaders().putAll(response.headers().asHttpHeaders());
                            return Mono.just(exception);
                        });
        }
        catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        catch (ValidationException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private WebClient.RequestHeadersSpec<?> buildRequestBody(ServiceMethod method, WebClient.RequestBodySpec request,
            Message body) throws IOException {

        val allFields = body.getAllFields();
        val uploadFileFields = allFields.values()
            .stream()
            .filter(value -> (value instanceof List<?> && !((List<?>) value).isEmpty()
                    && ((List<?>) value).get(0) instanceof GsvcExt.UploadFile) || value instanceof GsvcExt.UploadFile)
            .toList();

        WebClient.RequestHeadersSpec<?> response;
        if (uploadFileFields.isEmpty()) {
            val requestBody = objectMapper.writeValueAsString(body);
            response = request.contentType(MediaType.APPLICATION_JSON)
                .acceptCharset(StandardCharsets.UTF_8)
                .contentLength(requestBody.length())
                .bodyValue(requestBody);
        }
        else {
            val formData = generateMultipartFormData(allFields).build();
            log.trace("Form Data of {}: {}", method.getRpcAddr(), formData);
            response = request.contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(formData));
        }

        return response;
    }

    private MultipartBodyBuilder generateMultipartFormData(Map<Descriptors.FieldDescriptor, Object> allFields)
            throws IOException {
        val builder = new MultipartBodyBuilder();
        for (Descriptors.FieldDescriptor descriptor : allFields.keySet()) {
            val value = allFields.get(descriptor);
            val name = descriptor.getName();
            val header = String.format("form-data; name=\"%s\"", name);
            if (value instanceof GsvcExt.UploadFile uploadFile) {
                addFile(name, uploadFile, builder);
            }
            else if (value instanceof List<?> files) {
                if (files.isEmpty()) {
                    continue;
                }
                if (files.get(0) instanceof GsvcExt.UploadFile) {
                    for (Object file : files) {
                        addFile(name, (GsvcExt.UploadFile) file, builder);
                    }
                }
                else {
                    for (Object file : files) {
                        builder.part(name, file).header("Content-Disposition", header);
                    }
                }
            }
            else {
                builder.part(name, value).header("Content-Disposition", header);
            }
        }
        return builder;
    }

    private void addFile(String name, GsvcExt.UploadFile file, MultipartBodyBuilder builder) throws IOException {
        val originalFile = file.getFile();
        val of = new File(originalFile);
        val contentType = URLConnection.guessContentTypeFromName(originalFile);
        val header = String.format("form-data; name=\"%s\"; filename=\"%s\"", name, URLEncodeUtil.encode(of.getName()));
        val fileInputStream = DataBufferUtils.readInputStream(() -> new FileInputStream(of),
                DefaultDataBufferFactory.sharedInstance, 1024);

        val headers = new HttpHeaders();
        headers.add("Content-Disposition", header);

        val part = new FilePart(name, headers, fileInputStream);

        builder.part(name, part, MediaType.valueOf(contentType));
    }

    record FilePart(String name, HttpHeaders headers, Flux<DataBuffer> content) implements Part {
    }

}
