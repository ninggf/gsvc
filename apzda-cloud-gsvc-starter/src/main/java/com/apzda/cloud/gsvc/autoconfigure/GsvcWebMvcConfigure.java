package com.apzda.cloud.gsvc.autoconfigure;

import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.converter.EncryptedMessageConverter;
import com.apzda.cloud.gsvc.error.GsvcErrorAttributes;
import com.apzda.cloud.gsvc.error.GsvcErrorController;
import com.apzda.cloud.gsvc.exception.ExceptionTransformer;
import com.apzda.cloud.gsvc.exception.GsvcExceptionHandler;
import com.apzda.cloud.gsvc.filter.GsvcServletFilter;
import com.apzda.cloud.gsvc.resolver.PagerResolver;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.fasterxml.jackson.databind.Module;
import com.hubspot.jackson.datatype.protobuf.ProtobufJacksonConfig;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import reactor.core.publisher.Hooks;

import java.util.List;

/**
 * @author ninggf
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ ServiceConfigProperties.class })
@Import({ TracingFeignClientAutoConfiguration.class, TracingRestClientAutoConfiguration.class })
class GsvcWebMvcConfigure implements WebMvcConfigurer, InitializingBean {

    private final ServiceConfigProperties serviceConfigProperties;

    private final EncryptedMessageConverter encryptedMessageConverter;

    public GsvcWebMvcConfigure(ServiceConfigProperties serviceConfigProperties,
            EncryptedMessageConverter encryptedMessageConverter) {
        this.serviceConfigProperties = serviceConfigProperties;
        this.encryptedMessageConverter = encryptedMessageConverter;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        val config = serviceConfigProperties.getConfig();
        ResponseUtils.config(config);

        if (config.isContextCapture()) {
            Hooks.enableAutomaticContextPropagation();
        }
    }

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new PagerResolver(this.serviceConfigProperties.getConfig()));
    }

    @Override
    public void configureMessageConverters(@Nonnull List<HttpMessageConverter<?>> converters) {
        converters.add(0, encryptedMessageConverter);
    }

    @Bean
    static Module protobufModule(ServiceConfigProperties serviceConfigProperties) {
        val config = serviceConfigProperties.getConfig();
        val pbConfig = ProtobufJacksonConfig.builder()
            .acceptLiteralFieldnames(config.isAcceptLiteralFieldNames())
            .properUnsignedNumberSerialization(config.isProperUnsignedNumberSerialization())
            .serializeLongsAsString(config.isSerializeLongsAsString())
            .build();

        return new ProtobufModule(pbConfig);
    }

    @Bean
    GsvcExceptionHandler gsvcExceptionHandler(ObjectProvider<List<HttpMessageConverter<?>>> httpMessageConverters,
            ObjectProvider<List<ExceptionTransformer>> transformers) {
        return new GsvcExceptionHandler(httpMessageConverters, transformers);
    }

    @Bean
    @ConditionalOnMissingBean(value = ErrorAttributes.class, search = SearchStrategy.CURRENT)
    ErrorAttributes gsvcErrorAttributes(GsvcExceptionHandler handler) {
        return new GsvcErrorAttributes(handler);
    }

    @Bean
    @ConditionalOnMissingBean(value = ErrorController.class, search = SearchStrategy.CURRENT)
    BasicErrorController basicErrorController(ErrorAttributes errorAttributes,
            ObjectProvider<ErrorViewResolver> errorViewResolvers, ServerProperties serverProperties) {
        return new GsvcErrorController(errorAttributes, serverProperties.getError(),
                errorViewResolvers.orderedStream().toList());
    }

    @Bean
    FilterRegistrationBean<GsvcServletFilter> gsvcFilterRegistration(LocaleResolver localeResolver,
            @Value("${spring.application.name:main}") String appName) {
        FilterRegistrationBean<GsvcServletFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new GsvcServletFilter(localeResolver, appName));
        registration.addUrlPatterns("/*");
        registration.setName("gsvcServletFilter");
        registration.setOrder(-2147483646);

        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    MultipartResolver multipartResolver() {
        val resolver = new StandardServletMultipartResolver();

        resolver.setResolveLazily(true);
        log.trace("Use StandardServletMultipartResolver with ResolveLazily!");
        return resolver;
    }

}
