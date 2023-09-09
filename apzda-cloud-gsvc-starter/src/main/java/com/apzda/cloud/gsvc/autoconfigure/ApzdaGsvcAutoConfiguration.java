package com.apzda.cloud.gsvc.autoconfigure;

import com.apzda.cloud.gsvc.client.IServiceCaller;
import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.core.DefaultServiceCaller;
import com.apzda.cloud.gsvc.core.GatewayServiceBeanFactoryPostProcessor;
import com.apzda.cloud.gsvc.core.GatewayServiceRegistry;
import com.apzda.cloud.gsvc.core.ServiceConfigurationProperties;
import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.jackson.datatype.protobuf.ProtobufJacksonConfig;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * @author fengz
 */
@AutoConfiguration(before = { WebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class })
@Import({ ApzdaGsvcWebConfig.class })
@Slf4j
public class ApzdaGsvcAutoConfiguration implements SmartLifecycle, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private volatile boolean isRunning = false;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String appName = applicationContext.getEnvironment().getProperty("spring.application.name");
        if (!StringUtils.hasText(appName)) {
            throw new IllegalStateException("'spring.application.name' must be not empty!");
        }
        this.applicationContext = applicationContext;
        ServiceConfigurationProperties properties = applicationContext.getBean(ServiceConfigurationProperties.class);
        val config = properties.getConfig();
        val pbConfig = ProtobufJacksonConfig.builder()
            .acceptLiteralFieldnames(config.isAcceptLiteralFieldNames())
            .properUnsignedNumberSerialization(config.isProperUnsignedNumberSerialization())
            .serializeLongsAsString(config.isSerializeLongsAsString())
            .build();
        ResponseUtils.config(pbConfig);
        applicationContext.getBean(ObjectMapper.class).registerModule(new ProtobufModule(pbConfig));
    }

    @Override
    public void start() {
        for (Map.Entry<Class<?>, GatewayServiceRegistry.ServiceInfo> svc : GatewayServiceRegistry.DECLARED_SERVICES
            .entrySet()) {
            val service = svc.getValue();
            val interfaceName = svc.getKey();
            val name = service.getServiceName();
            val bean = applicationContext.getBean(interfaceName);
            GatewayServiceRegistry.register(interfaceName, bean);
        }
        isRunning = true;
        log.debug("Gsvc is ready!");
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public int getPhase() {
        return 100;
    }

    @Bean
    GatewayServiceConfigure gatewayServiceConfigure(ServiceConfigurationProperties properties) {
        return new GatewayServiceConfigure(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    IServiceCaller serviceCaller(WebClient webClient, ApplicationContext applicationContext,
            GatewayServiceConfigure serviceConfigure) {
        return new DefaultServiceCaller(webClient, applicationContext, serviceConfigure);
    }

    @Bean
    BeanFactoryPostProcessor gsvcBeanFactoryPostProcessor() {
        return new GatewayServiceBeanFactoryPostProcessor();
    }

}
