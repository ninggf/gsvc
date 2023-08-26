package com.apzda.cloud.gsvc.filter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.val;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * @author ninggf
 */
@Data
public class XForwardedHeadersFilter {

    /**
     * Default http port.
     */
    public static final int HTTP_PORT = 80;

    /**
     * Default https port.
     */
    public static final int HTTPS_PORT = 443;

    /**
     * Http url scheme.
     */
    public static final String HTTP_SCHEME = "http";

    /**
     * Https url scheme.
     */
    public static final String HTTPS_SCHEME = "https";

    /**
     * X-Forwarded-For Header.
     */
    public static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

    /**
     * X-Forwarded-Host Header.
     */
    public static final String X_FORWARDED_HOST_HEADER = "X-Forwarded-Host";

    /**
     * X-Forwarded-Port Header.
     */
    public static final String X_FORWARDED_PORT_HEADER = "X-Forwarded-Port";

    /**
     * X-Forwarded-Proto Header.
     */
    public static final String X_FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";

    /**
     * X-Forwarded-Prefix Header.
     */
    public static final String X_FORWARDED_PREFIX_HEADER = "X-Forwarded-Prefix";

    /**
     * The order of the XForwardedHeadersFilter.
     */
    private int order = 0;

    /**
     * If the XForwardedHeadersFilter is enabled.
     */
    private boolean enabled = true;

    /**
     * If X-Forwarded-For is enabled.
     */
    private boolean forEnabled = true;

    /**
     * If X-Forwarded-Host is enabled.
     */
    private boolean hostEnabled = true;

    /**
     * If X-Forwarded-Port is enabled.
     */
    private boolean portEnabled = true;

    /**
     * If X-Forwarded-Proto is enabled.
     */
    private boolean protoEnabled = true;

    /**
     * If X-Forwarded-Prefix is enabled.
     */
    private boolean prefixEnabled = true;

    /**
     * If appending X-Forwarded-For as a list is enabled.
     */
    private boolean forAppend = true;

    /**
     * If appending X-Forwarded-Host as a list is enabled.
     */
    private boolean hostAppend = true;

    /**
     * If appending X-Forwarded-Port as a list is enabled.
     */
    private boolean portAppend = true;

    /**
     * If appending X-Forwarded-Proto as a list is enabled.
     */
    private boolean protoAppend = true;

    /**
     * If appending X-Forwarded-Prefix as a list is enabled.
     */
    private boolean prefixAppend = true;

    public HttpHeaders filter(HttpHeaders input, HttpServletRequest request) {

        HttpHeaders updated = new HttpHeaders();

        for (Map.Entry<String, List<String>> entry : input.entrySet()) {
            updated.addAll(entry.getKey(), entry.getValue());
        }
        val remoteAddress = request.getRemoteAddr();

        if (isForEnabled() && remoteAddress != null) {
            write(updated, X_FORWARDED_FOR_HEADER, remoteAddress, isForAppend());
        }
        try {
            val uri = new URI(request.getRequestURI());
            String proto = uri.getScheme();
            if (isProtoEnabled()) {
                write(updated, X_FORWARDED_PROTO_HEADER, proto, isProtoAppend());
            }

            if (isPortEnabled()) {
                String port = String.valueOf(uri.getPort());
                if (uri.getPort() < 0) {
                    port = String.valueOf(getDefaultPort(proto));
                }
                write(updated, X_FORWARDED_PORT_HEADER, port, isPortAppend());
            }

            if (isHostEnabled()) {
                String host = toHostHeader(uri);
                write(updated, X_FORWARDED_HOST_HEADER, host, isHostAppend());
            }
        }
        catch (URISyntaxException e) {
            // nothing to do
        }

        return updated;
    }

    private static String substringBeforeLast(String str, String separator) {
        if (ObjectUtils.isEmpty(str) || ObjectUtils.isEmpty(separator)) {
            return str;
        }
        int pos = str.lastIndexOf(separator);
        if (pos == -1) {
            return str;
        }
        return str.substring(0, pos);
    }

    private void write(HttpHeaders headers, String name, String value, boolean append) {
        if (value == null) {
            return;
        }
        if (append) {
            headers.add(name, value);
            // these headers should be treated as a single comma separated header
            List<String> values = headers.get(name);
            String delimitedValue = StringUtils.collectionToCommaDelimitedString(values);
            headers.set(name, delimitedValue);
        }
        else {
            headers.set(name, value);
        }
    }

    private int getDefaultPort(String scheme) {
        return HTTPS_SCHEME.equals(scheme) ? HTTPS_PORT : HTTP_PORT;
    }

    private boolean hasHeader(ServerHttpRequest request, String name) {
        HttpHeaders headers = request.getHeaders();
        return headers.containsKey(name) && StringUtils.hasLength(headers.getFirst(name));
    }

    private String toHostHeader(URI uri) {
        int port = uri.getPort();
        String host = uri.getHost();
        String scheme = uri.getScheme();
        if (port < 0 || (port == HTTP_PORT && HTTP_SCHEME.equals(scheme))
                || (port == HTTPS_PORT && HTTPS_SCHEME.equals(scheme))) {
            return host;
        }
        else {
            return host + ":" + port;
        }
    }

    private String stripTrailingSlash(URI uri) {
        if (uri.getPath().endsWith("/")) {
            return uri.getPath().substring(0, uri.getPath().length() - 1);
        }
        else {
            return uri.getPath();
        }
    }

}
