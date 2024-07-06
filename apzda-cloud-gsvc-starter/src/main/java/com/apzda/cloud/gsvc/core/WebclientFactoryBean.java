package com.apzda.cloud.gsvc.core;

import com.apzda.cloud.gsvc.config.GatewayServiceConfigure;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

/**
 * @author fengz
 */
public class WebclientFactoryBean implements FactoryBean<WebClient>, ApplicationContextAware {

    private static final Logger webLog = LoggerFactory.getLogger("org.springframework.web");

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
        val baseUrl = ServiceMethod.getServiceBaseUrl(svcLbName);
        builder.baseUrl(baseUrl);

        val readTimeout = svcConfigure.getReadTimeout(cfgName, true);
        val writeTimeout = svcConfigure.getWriteTimeout(cfgName, true);
        val connectTimeout = svcConfigure.getConnectTimeout(cfgName);

        val httpClient = HttpClient.create()
            .followRedirect(false)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .doOnConnected(conn -> {
                if (readTimeout.toMillis() > 0) {
                    conn.addHandlerLast(new ReadTimeoutHandler(readTimeout.toMillis(), TimeUnit.MILLISECONDS));
                }
                if (writeTimeout.toMillis() > 0) {
                    conn.addHandlerLast(new WriteTimeoutHandler(writeTimeout.toMillis(), TimeUnit.MILLISECONDS));
                }
            });

        if (connectTimeout.toMillis() > 0) {
            httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis());
        }

        val connector = new ReactorClientHttpConnector(httpClient);

        webLog.debug("Setup WebClient for {}: BASE={}, ConnectTimeout={}, ReadTimeout={}, WriteTimeout={}", cfgName,
                baseUrl, connectTimeout, readTimeout, writeTimeout);

        return builder.clientConnector(connector).build();
    }

    @Override
    public Class<?> getObjectType() {
        return WebClient.class;
    }

}
