package com.apzda.cloud.gsvc.listener;

import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.ServletRequestHandledEvent;

@Component("LongResponseTimeListener")
@ConditionalOnProperty(name = "apzda.cloud.config.log-slow", havingValue = "true")
public class ServletRequestHandledEventListener implements ApplicationListener<ServletRequestHandledEvent> {

    private static final Logger log = LoggerFactory.getLogger("slow");

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
