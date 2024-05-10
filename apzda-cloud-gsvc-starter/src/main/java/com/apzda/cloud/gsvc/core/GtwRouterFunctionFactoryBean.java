package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.dto.MessageType;
import com.apzda.cloud.gsvc.dto.Response;
import com.apzda.cloud.gsvc.error.ServiceError;
import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import com.apzda.cloud.gsvc.gtw.ProxyExchangeHandler;
import com.apzda.cloud.gsvc.gtw.Route;
import com.apzda.cloud.gsvc.gtw.RouteRegistry;
import com.apzda.cloud.gsvc.server.IServiceMethodHandler;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.apzda.cloud.swagger.ProtobufMsgHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.function.*;

import java.util.List;

import static org.springdoc.core.utils.Constants.OPERATION_ATTRIBUTE;

/**
 * @author fengz
 */
@Slf4j
@RequiredArgsConstructor
public class GtwRouterFunctionFactoryBean
        implements FactoryBean<RouterFunction<ServerResponse>>, ApplicationContextAware {

    public static final String ATTR_MATCHED_SEGMENTS = "GSVC_ATTR_MATCHED_SEGMENTS";

    private final Route route;

    private final Class<?> serviceClass;

    @Value("${server.servlet.context-path:/}")
    private String servletContext;

    private ApplicationContext applicationContext;

    private ProxyExchangeHandler proxyExchangeHandler;

    private IServiceMethodHandler serviceMethodHandler;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.proxyExchangeHandler = applicationContext.getBean(ProxyExchangeHandler.class);
        this.serviceMethodHandler = applicationContext.getBean(IServiceMethodHandler.class);
    }

    @Override
    public RouterFunction<ServerResponse> getObject() throws Exception {
        servletContext = StringUtils.stripEnd(servletContext, "/");
        val serviceInfo = GatewayServiceRegistry.getServiceInfo(serviceClass);
        val router = RouterFunctions.route();
        if ("http".equals(serviceInfo.type)) {
            setupForward(router, route, serviceInfo);
        }
        else {
            setupRoute(router, route, serviceInfo);
        }
        setupFilter(router, route);

        val path = route.absPath();
        val meta = route.meta();
        if (meta.isLogin()) {
            GatewayServiceRegistry.registerRouteMeta(path, meta);
        }

        val exceptionHandler = applicationContext.getBean(GsvcExceptionHandler.class);
        // bookmark exception handle(gtw call)
        return router.onError(Exception.class, exceptionHandler::handle).build();
    }

    private void setupRoute(@NonNull RouterFunctions.Builder builder, @NonNull Route route, ServiceInfo serviceInfo) {
        val actions = route.getActions();
        val serviceMethod = getServiceMethod(route, serviceInfo);
        val path = route.absPath();
        val method = serviceMethod.getDmName();
        if (log.isDebugEnabled()) {
            log.debug("SN Route {} to {}.{}({})", path, serviceMethod.getServiceName(), method, route.meta());
        }

        HandlerFunction<ServerResponse> func = request -> {
            val type = serviceMethod.getType();

            return switch (type) {
                case UNARY -> serviceMethodHandler.handleUnary(request, serviceClass, method, null);
                case SERVER_STREAMING ->
                    serviceMethodHandler.handleServerStreaming(request, serviceClass, method, null);
                default -> serviceMethodHandler.handleBidStreaming(request, serviceClass, method, null);
            };
        };

        builder.route((request -> match(request, path, actions)), func);

        val apiDocEnabled = applicationContext.getEnvironment()
            .getProperty("springdoc.api-docs.enabled", Boolean.class, true);

        if (apiDocEnabled) {
            try {
                val operationBuilder = createOperationBuilder(route, serviceMethod);
                builder.withAttribute(OPERATION_ATTRIBUTE, operationBuilder);
            }
            catch (Exception e) {
                log.warn("Cannot create swagger document for: {} - {}", path, e.getMessage());
            }
        }
    }

    private void setupForward(RouterFunctions.Builder builder, Route route, ServiceInfo serviceInfo) {
        val actions = route.getActions();
        val path = route.absPath();

        if (log.isDebugEnabled()) {
            log.debug("FW Route {} to {}.{}({})", path, serviceInfo.getServiceName(), route.getMethod(), route.meta());
        }

        final HandlerFunction<ServerResponse> func = request -> proxyExchangeHandler.handle(request, route,
                serviceInfo);

        builder.route(request -> match(request, path, actions), func);

        RouteRegistry.register(path);
    }

    @SuppressWarnings("unchecked")
    private void setupFilter(RouterFunctions.Builder router, Route route) {
        val svcConfigure = applicationContext.getBean(GatewayServiceConfigure.class);
        val globalFilters = svcConfigure.getGlobalFilters();
        if (!globalFilters.isEmpty()) {
            log.debug("Setup global filters for {}: {}", route, globalFilters);
            for (HandlerFilterFunction<ServerResponse, ServerResponse> filter : globalFilters) {
                router.filter(filter);
            }
        }
        var filters = route.getFilters();

        if (filters.isEmpty()) {
            return;
        }

        log.debug("Setup filters for {}: {}", route, filters);

        val filtersBean = filters.stream()
            .map(filter -> applicationContext.getBean(filter, HandlerFilterFunction.class))
            .toList();

        if (!CollectionUtils.isEmpty(filtersBean)) {
            for (HandlerFilterFunction<ServerResponse, ServerResponse> filter : filtersBean) {
                router.filter(filter);
            }
        }
    }

    private ServiceMethod getServiceMethod(Route route, ServiceInfo serviceInfo) {
        val method = route.getMethod();
        val methods = GatewayServiceRegistry.getDeclaredServiceMethods(serviceInfo);
        val serviceMethod = methods.get(method);
        if (serviceMethod == null) {
            throw new IllegalStateException(String.format("method of %s is not found", route));
        }
        return serviceMethod;
    }

    @Override
    public Class<?> getObjectType() {
        return RouterFunction.class;
    }

    private boolean match(ServerRequest request, String path, List<HttpMethod> actions) {
        val reqPath = request.path();
        boolean matched = RouteRegistry.pathMatcher.match(path, reqPath);
        if (matched && !actions.isEmpty()) {
            matched = actions.contains(request.method());
        }
        val httpServletRequest = request.servletRequest();

        if (matched && httpServletRequest.getAttribute(ATTR_MATCHED_SEGMENTS) == null) {
            val segments = RouteRegistry.pathMatcher.extractUriTemplateVariables(path, reqPath);
            val segment = RouteRegistry.pathMatcher.extractPathWithinPattern(path, reqPath);
            segments.put("segment", segment);
            httpServletRequest.setAttribute(ATTR_MATCHED_SEGMENTS, segments);
            httpServletRequest.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, path);
            log.trace("{} matched '{}' with segments: {}", reqPath, path, segments);
        }
        return matched;
    }

    private Builder createOperationBuilder(Route route, ServiceMethod serviceMethod) throws JsonProcessingException {
        val ro = Builder.operationBuilder();
        // ro.beanClass(serviceMethod.getInterfaceName()).beanMethod(serviceMethod.getDmName());
        ro.operationId(route.absPath());
        ro.tag(serviceMethod.getServiceName());
        val summary = route.getSummary();
        if (StringUtils.isNotBlank(summary)) {
            ro.summary(summary);
        }

        val desc = route.getDesc();
        if (StringUtils.isNotBlank(desc)) {
            ro.description(desc);
        }

        val tags = route.getTags();
        if (tags != null && tags.length > 0) {
            ro.tags(tags);
        }

        // request
        ProtobufMsgHelper.create(serviceMethod.getRequestType());
        val requestBodyBuilder = org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder();
        requestBodyBuilder.required(true);
        requestBodyBuilder.content(newBuilder("Request", serviceMethod.getRequestType()));
        ro.requestBody(requestBodyBuilder);
        // response
        val responseBuilder = org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder();
        responseBuilder.content(newBuilder("Response", serviceMethod.getReturnType()));
        responseBuilder.responseCode("200");
        responseBuilder.description("Success Response");
        ro.response(responseBuilder);
        val errResponseBuilder = org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder();
        errResponseBuilder.content(newBuilder("Error", serviceMethod.getReturnType()));
        errResponseBuilder.responseCode("!200");
        errResponseBuilder.description("Error Response");
        ro.response(errResponseBuilder);

        if (route.meta().isLogin()) {
            val parameter = applicationContext.getEnvironment()
                .getProperty("apzda.cloud.security.token-name", String.class, "Authorization");
            val bearer = applicationContext.getEnvironment()
                .getProperty("apzda.cloud.security.bearer", String.class, "Bearer");
            val parameterBuilder = org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder();
            parameterBuilder.in(ParameterIn.HEADER);
            parameterBuilder.required(true);
            parameterBuilder.allowEmptyValue(false);
            parameterBuilder.name(parameter);
            parameterBuilder.example(bearer + " 1234567890");
            ro.parameter(parameterBuilder);

            ro.security(org.springdoc.core.fn.builders.securityrequirement.Builder.securityRequirementBuilder()
                .name("JWT"));
        }
        return ro;
    }

    private org.springdoc.core.fn.builders.content.Builder newBuilder(String title, Class<?> contentClass)
            throws JsonProcessingException {
        val contentBuilder = org.springdoc.core.fn.builders.content.Builder.contentBuilder();
        contentBuilder.mediaType(MediaType.APPLICATION_JSON_VALUE);
        val schemaBuilder = org.springdoc.core.fn.builders.schema.Builder.schemaBuilder();
        schemaBuilder.type("object");
        schemaBuilder.nullable(false);
        schemaBuilder.required(true);
        schemaBuilder.title(title);
        schemaBuilder.description("Class: " + contentClass.getName());
        val defaultValue = ProtobufMsgHelper.create(contentClass);
        if (title.equals("Response")) {
            schemaBuilder.defaultValue(ResponseUtils.OBJECT_MAPPER.writeValueAsString(Response.success(defaultValue)));
        }
        else if (title.equals("Error")) {
            schemaBuilder.description("Error Response");

            schemaBuilder.defaultValue(ResponseUtils.OBJECT_MAPPER
                .writeValueAsString(Response.error(ServiceError.SERVICE_UNAVAILABLE).type(MessageType.NOTIFY)));

            schemaBuilder.externalDocs(
                    org.springdoc.core.fn.builders.externaldocumentation.Builder.externalDocumentationBuilder()
                        .url("https://gsvc.apzda.com/manual/errors"));
        }
        else {
            schemaBuilder.defaultValue(ResponseUtils.OBJECT_MAPPER.writeValueAsString(defaultValue));
        }
        contentBuilder.schema(schemaBuilder);
        return contentBuilder;
    }

}
