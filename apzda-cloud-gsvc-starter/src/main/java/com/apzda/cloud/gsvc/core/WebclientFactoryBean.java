package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author fengz
 */
@Slf4j
public class WebclientFactoryBean implements FactoryBean<WebClient>, ApplicationContextAware {

    private final String cfgName;

    private WebClient.Builder builder;

    private GatewayServiceConfigure svcConfigure;

    public WebclientFactoryBean(String cfgName) {
        this.cfgName = cfgName;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.builder = applicationContext.getBean(WebClient.Builder.class);
        this.svcConfigure = applicationContext.getBean(GatewayServiceConfigure.class);
        try {
            val lbFunction = applicationContext.getBean(ReactorLoadBalancerExchangeFilterFunction.class);
            this.builder.filter(lbFunction);
        }
        catch (Exception e) {
            log.trace("lb is not work for {}.", cfgName);
        }
    }

    @Override
    public WebClient getObject() throws Exception {
        val readTimeout = svcConfigure.getReadTimeout(cfgName, true);
        val connectTimeout = svcConfigure.getConnectTimeout(cfgName);
        // val connector = new ReactorClientHttpConnector();
        log.trace("Setup WebClient for {}: ReadTimeout={},ConnectTimeout={}", cfgName, readTimeout, connectTimeout);
        // TODO 设置连接和读取超时时间
        return this.builder.build();
    }

    @Override
    public Class<?> getObjectType() {
        return WebClient.class;
    }

}
