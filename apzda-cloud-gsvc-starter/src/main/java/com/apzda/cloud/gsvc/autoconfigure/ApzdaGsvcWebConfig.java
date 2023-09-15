package com.apzda.cloud.gsvc.autoconfigure;

import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.error.GsvcErrorAttributes;
import com.apzda.cloud.gsvc.error.GsvcErrorController;
import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import com.apzda.cloud.gsvc.filter.GsvcServletFilter;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.jackson.datatype.protobuf.ProtobufJacksonConfig;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import jakarta.servlet.Filter;
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
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;

import java.util.List;

/**
 * @author ninggf
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ServiceConfigProperties.class)
public class ApzdaGsvcWebConfig implements InitializingBean {

    private final ServiceConfigProperties serviceConfigProperties;

    private final ObjectMapper objectMapper;

    public ApzdaGsvcWebConfig(ServiceConfigProperties serviceConfigProperties, ObjectMapper objectMapper) {
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
        ResponseUtils.config(pbConfig, config);
        log.trace("ResponseUtils configured: {}", config);
        objectMapper.registerModule(new ProtobufModule(pbConfig));
        log.trace("ObjectMapper register ProtobufModule with config: {}", config);
    }

    @Bean
    GsvcExceptionHandler gsvcExceptionHandler(ServiceConfigProperties properties,
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
    public BasicErrorController basicErrorController(ErrorAttributes errorAttributes,
            ServiceConfigProperties properties, ObjectProvider<ErrorViewResolver> errorViewResolvers,
            ServerProperties serverProperties) {
        return new GsvcErrorController(errorAttributes, serverProperties.getError(), properties,
                errorViewResolvers.orderedStream().toList());
    }

    @Bean
    FilterRegistrationBean<Filter> gsvcFilterRegistration() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new GsvcServletFilter());
        registration.addUrlPatterns("/*");
        registration.setName("gsvcServletFilter");
        registration.setOrder(1);

        return registration;
    }

}
