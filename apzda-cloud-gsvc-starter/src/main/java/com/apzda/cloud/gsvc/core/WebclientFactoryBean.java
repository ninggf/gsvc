package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

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
        val svcLbName = svcConfigure.svcLbName(cfgName);
        val baseUrl = ServiceMethod.baseUrl(svcLbName);
        try {
            Class.forName(
                    "org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction");

            this.builder.filter(applicationContext.getBean(
                    org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction.class));
            log.debug("LoadBalancer is enabled for {}. BASE URI: {}", cfgName, baseUrl);
        }
        catch (Exception e) {
            log.debug("LoadBalancer is disabled for {}. BASE URI: {}", cfgName, baseUrl);
        }
        this.builder.baseUrl(baseUrl);
    }

    @Override
    public WebClient getObject() throws Exception {
        val readTimeout = svcConfigure.getReadTimeout(cfgName, true);
        val writeTimeout = svcConfigure.getWriteTimeout(cfgName, true);
        val connectTimeout = svcConfigure.getConnectTimeout(cfgName);

        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis())
            .option(ChannelOption.SO_KEEPALIVE, true)
            .doOnConnected(conn -> {
                conn.addHandlerLast(new ReadTimeoutHandler(readTimeout.toMillis(), TimeUnit.MILLISECONDS));
                if (!writeTimeout.isZero() && writeTimeout.isNegative()) {
                    conn.addHandlerLast(new WriteTimeoutHandler(writeTimeout.toMillis(), TimeUnit.MILLISECONDS));
                }
            });

        val connector = new ReactorClientHttpConnector(httpClient);

        log.trace("[{}] Setup WebClient for {}: ConnectTimeout={}, ReadTimeout={}, WriteTimeout={}",
                GsvcContextHolder.getRequestId(), cfgName, connectTimeout, readTimeout, writeTimeout);
        // bookmark 设置连接和读取超时时间
        return this.builder.clientConnector(connector).build();
    }

    @Override
    public Class<?> getObjectType() {
        return WebClient.class;
    }

}