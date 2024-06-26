package com.apzda.cloud.gsvc.gtw.filter;

import lombok.val;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;

import java.util.List;

public interface HttpHeadersFilter {

    static HttpHeaders filterRequest(List<HttpHeadersFilter> filters, ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        return filter(filters, headers, request, Type.REQUEST);
    }

    static HttpHeaders filter(List<HttpHeadersFilter> filters, HttpHeaders input, ServerHttpRequest request,
            Type type) {
        if (filters != null) {
            HttpHeaders filtered = input;
            for (val filter : filters) {
                if (filter.supports(type)) {
                    filtered = filter.filter(filtered, request);
                }
            }
            return filtered;
        }

        return input;
    }

    /**
     * Filters a set of Http Headers.
     * @return filtered Http Headers
     */
    HttpHeaders filter(HttpHeaders input, ServerHttpRequest request);

    default boolean supports(Type type) {
        return type.equals(Type.REQUEST);
    }

    enum Type {

        /**
         * Filter for request headers.
         */
        REQUEST,

        /**
         * Filter for response headers.
         */
        RESPONSE

    }

}
