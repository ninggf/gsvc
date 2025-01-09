package com.apzda.cloud.gsvc.tracing;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TracingRestTemplateInterceptorAfterPropertiesSet implements InitializingBean {

    private final Collection<RestTemplate> restTemplates;

    private final TracingRestTemplateInterceptor tracingRestTemplateInterceptor;

    public TracingRestTemplateInterceptorAfterPropertiesSet(
            TracingRestTemplateInterceptor tracingRestTemplateInterceptor,
            ObjectProvider<RestTemplate> restTemplateProvider) {
        this.tracingRestTemplateInterceptor = tracingRestTemplateInterceptor;
        restTemplates = restTemplateProvider.orderedStream().toList();
    }

    public void afterPropertiesSet() {
        if (!CollectionUtils.isEmpty(restTemplates)) {
            for (RestTemplate restTemplate : this.restTemplates) {
                final List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());
                interceptors.add(this.tracingRestTemplateInterceptor);
                restTemplate.setInterceptors(interceptors);
            }
        }

    }

}
