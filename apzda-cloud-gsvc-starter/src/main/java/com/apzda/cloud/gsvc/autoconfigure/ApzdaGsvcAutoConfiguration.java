package com.apzda.cloud.gsvc.autoconfigure;

import com.apzda.cloud.gsvc.ResponseUtils;
import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import com.apzda.cloud.gsvc.core.GatewayServiceBeanFactoryPostProcessor;
import com.apzda.cloud.gsvc.core.GatewayServiceRegistry;
import com.apzda.cloud.gsvc.core.SaTokenExtendProperties;
import com.apzda.cloud.gsvc.core.ServiceConfigurationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.jackson.datatype.protobuf.ProtobufJacksonConfig;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;

/**
 * @author fengz
 */
@AutoConfiguration(after = WebMvcAutoConfiguration.class)
@EnableConfigurationProperties({ServiceConfigurationProperties.class, SaTokenExtendProperties.class})
@Import({ApzdaGsvcWebConfig.class})
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
    }

    @Override
    public void start() {
        ServiceConfigurationProperties properties = applicationContext.getBean(ServiceConfigurationProperties.class);
        val config = properties.getConfig();
        val pbConfig = ProtobufJacksonConfig.builder()
            .acceptLiteralFieldnames(config.isAcceptLiteralFieldNames())
            .properUnsignedNumberSerialization(config.isProperUnsignedNumberSerialization())
            .serializeLongsAsString(config.isSerializeLongsAsString())
            .build();
        ResponseUtils.config(pbConfig);
        applicationContext.getBean(ObjectMapper.class).registerModule(new ProtobufModule(pbConfig));

        val appName = applicationContext.getEnvironment().getProperty("spring.application.name");
        val service = properties.getService();
        if (service != null) {
            val interfaceName = service.getInterfaceName();
            val name = service.getName();
            val bean = applicationContext.getBean(interfaceName);
            GatewayServiceRegistry.register(appName, name, -1, bean, interfaceName);
        }

        val references = properties.getReference();
        for (int i = 0; i < references.size(); i++) {
            val rc = references.get(i);
            val name = rc.getName();
            val interfaceName = rc.getInterfaceName();
            val bean = applicationContext.getBean(interfaceName);
            if (GatewayServiceRegistry.isLocalService(i)) {
                GatewayServiceRegistry.register(appName, name, i, bean, interfaceName);
            } else {
                GatewayServiceRegistry.register(rc.getApp(), name, i, bean, interfaceName);
            }
        }

        isRunning = true;
        log.info("Gsvc started");
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
    BeanFactoryPostProcessor gsvcBeanFactoryPostProcessor() {
        return new GatewayServiceBeanFactoryPostProcessor();
    }
}
