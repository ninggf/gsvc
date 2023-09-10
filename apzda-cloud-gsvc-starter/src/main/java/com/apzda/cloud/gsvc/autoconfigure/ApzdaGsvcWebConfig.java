package com.apzda.cloud.gsvc.autoconfigure;

import com.apzda.cloud.gsvc.config.SaTokenExtendProperties;
import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.error.GsvcErrorAttributes;
import com.apzda.cloud.gsvc.error.GsvcErrorController;
import com.apzda.cloud.gsvc.exception.handler.GsvcExceptionHandler;
import com.apzda.cloud.gsvc.filter.GsvcFilter;
import com.apzda.cloud.gsvc.gtw.filter.LoginFilter;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.jackson.datatype.protobuf.ProtobufJacksonConfig;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author ninggf
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ ServiceConfigProperties.class, SaTokenExtendProperties.class })
public class ApzdaGsvcWebConfig implements InitializingBean {

    private final ServiceConfigProperties serviceConfigProperties;

    private final ObjectMapper objectMapper;

    public ApzdaGsvcWebConfig(ApplicationContext applicationContext, ServiceConfigProperties serviceConfigProperties,
            ObjectMapper objectMapper) {
        this.serviceConfigProperties = serviceConfigProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        val config = serviceConfigProperties.getConfig();
        val pbConfig = ProtobufJacksonConfig.builder()
            .acceptLiteralFieldnames(config.isAcceptLiteralFieldNames())
            .properUnsignedNumberSerialization(config.isProperUnsignedNumberSerialization())
            .serializeLongsAsString(config.isSerializeLongsAsString())
            .build();
        ResponseUtils.config(pbConfig);
        objectMapper.registerModule(new ProtobufModule(pbConfig));
    }

    @Bean("login")
    @ConditionalOnMissingBean(name = "login")
    HandlerFilterFunction<ServerResponse, ServerResponse> login() {
        return new LoginFilter();
    }

    @Bean
    GsvcExceptionHandler gsvcExceptionHandler(SaTokenExtendProperties properties,
            ObjectProvider<List<HttpMessageConverter<?>>> httpMessageConverters) {
        return new GsvcExceptionHandler(properties, httpMessageConverters);
    }

    @Bean
    @ConditionalOnMissingBean(value = ErrorAttributes.class, search = SearchStrategy.CURRENT)
    ErrorAttributes gsvcErrorAttributes(GsvcExceptionHandler handler) {
        return new GsvcErrorAttributes(handler);
    }

    @Bean
    @ConditionalOnMissingBean(value = ErrorController.class, search = SearchStrategy.CURRENT)
    public BasicErrorController basicErrorController(ErrorAttributes errorAttributes, GsvcExceptionHandler handler,
            ObjectProvider<ErrorViewResolver> errorViewResolvers, ServerProperties serverProperties) {
        return new GsvcErrorController(errorAttributes, serverProperties.getError(), handler,
                errorViewResolvers.orderedStream().toList());
    }

    @Bean
    GsvcFilter gsvcFilter() {
        return new GsvcFilter();
    }

}
