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
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

/**
 * @author fengz
 */
@Slf4j
public class WebclientFactoryBean implements FactoryBean<WebClient>, ApplicationContextAware {

    private final String cfgName;

    private ApplicationContext applicationContext;

    public WebclientFactoryBean(String cfgName) {
        this.cfgName = cfgName;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public WebClient getObject() throws Exception {
        WebClient.Builder builder = applicationContext.getBean(WebClient.Builder.class);
        GatewayServiceConfigure svcConfigure = applicationContext.getBean(GatewayServiceConfigure.class);

        val svcLbName = svcConfigure.svcLbName(cfgName);
        val baseUrl = ServiceMethod.baseUrl(svcLbName);
        var lb = "N/A";

        ExchangeFilterFunction lbFunc = null;
        if (this.applicationContext.containsBean("retryableLoadBalancerExchangeFilterFunction")) {
            lbFunc = this.applicationContext.getBean("retryableLoadBalancerExchangeFilterFunction",
                    ExchangeFilterFunction.class);
            lb = "Retryable";
        }
        else if (this.applicationContext.containsBean("loadBalancerExchangeFilterFunction")) {
            lbFunc = this.applicationContext.getBean("loadBalancerExchangeFilterFunction",
                    ExchangeFilterFunction.class);
            lb = "Simple";
        }
        if (lbFunc != null) {
            builder.filter(lbFunc);
        }

        builder.baseUrl(baseUrl);

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

        log.trace("[{}] Setup WebClient for {}: BASE={}, LB={}, ConnectTimeout={}, ReadTimeout={}, WriteTimeout={}",
                GsvcContextHolder.getRequestId(), cfgName, baseUrl, lb, connectTimeout, readTimeout, writeTimeout);

        return builder.clientConnector(connector).build();
    }

    @Override
    public Class<?> getObjectType() {
        return WebClient.class;
    }

}
