package com.apzda.cloud.gsvc.listener;

import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.ServletRequestHandledEvent;

@Component("LongResponseTimeListener")
@Slf4j
@ConditionalOnProperty(name = "apzda.cloud.config.log-slow", havingValue = "true")
public class ServletRequestHandledEventListener implements ApplicationListener<ServletRequestHandledEvent> {

    private final long duration;

    public ServletRequestHandledEventListener(ServiceConfigProperties properties) {
        this.duration = properties.getConfig().getSlowThrottle().toMillis();
    }

    @Override
    public void onApplicationEvent(@NonNull ServletRequestHandledEvent event) {
        if (duration > 0 && event.getProcessingTimeMillis() > duration) {
            log.warn("Slow Response detected: [{}]{} - {}ms", event.getMethod(), event.getRequestUrl(),
                    event.getProcessingTimeMillis());
        }
    }

}
