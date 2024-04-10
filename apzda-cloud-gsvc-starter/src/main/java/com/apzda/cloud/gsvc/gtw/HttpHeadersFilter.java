package com.apzda.cloud.gsvc.gtw;

import lombok.val;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.List;

public interface HttpHeadersFilter {

    static HttpHeaders filterRequest(List<HttpHeadersFilter> filters, ServerRequest request) {
        HttpHeaders headers = request.headers().asHttpHeaders();
        return filter(filters, headers, request, Type.REQUEST);
    }

    static HttpHeaders filter(List<HttpHeadersFilter> filters, HttpHeaders input, ServerRequest request, Type type) {
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
    HttpHeaders filter(HttpHeaders input, ServerRequest request);

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
