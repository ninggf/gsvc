/*
 * Copyright (C) 2023-2023 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.apzda.cloud.gsvc.gtw.filter;

import lombok.val;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForwardedHeadersFilter implements HttpHeadersFilter, Ordered {

    /**
     * Forwarded header.
     */
    public static final String FORWARDED_HEADER = "Forwarded";

    static List<Forwarded> parse(List<String> values) {
        ArrayList<Forwarded> forwardeds = new ArrayList<>();
        if (CollectionUtils.isEmpty(values)) {
            return forwardeds;
        }
        for (String value : values) {
            Forwarded forwarded = parse(value);
            forwardeds.add(forwarded);
        }
        return forwardeds;
    }

    static Forwarded parse(String value) {
        String[] pairs = StringUtils.tokenizeToStringArray(value, ";");

        LinkedCaseInsensitiveMap<String> result = splitIntoCaseInsensitiveMap(pairs);
        if (result == null) {
            return null;
        }

        return new Forwarded(result);
    }

    static LinkedCaseInsensitiveMap<String> splitIntoCaseInsensitiveMap(String[] pairs) {
        if (ObjectUtils.isEmpty(pairs)) {
            return null;
        }

        LinkedCaseInsensitiveMap<String> result = new LinkedCaseInsensitiveMap<>();
        for (String element : pairs) {
            String[] splittedElement = StringUtils.split(element, "=");
            if (splittedElement == null) {
                continue;
            }
            result.put(splittedElement[0].trim(), splittedElement[1].trim());
        }
        return result;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public HttpHeaders filter(HttpHeaders input, ServerHttpRequest request) {
        HttpHeaders updated = new HttpHeaders();

        // copy all headers except Forwarded
        for (Map.Entry<String, List<String>> entry : input.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase(FORWARDED_HEADER)) {
                updated.addAll(entry.getKey(), entry.getValue());
            }
        }

        List<Forwarded> forwardeds = parse(input.get(FORWARDED_HEADER));

        for (Forwarded f : forwardeds) {
            updated.add(FORWARDED_HEADER, f.toHeaderValue());
        }

        // TODO: add new forwarded
        URI uri = request.getURI();
        String host = input.getFirst(HttpHeaders.HOST);
        Forwarded forwarded = new Forwarded().put("host", host).put("proto", uri.getScheme());
        val remoteAddress = request.getRemoteAddress();
        if (remoteAddress.getAddress() != null) {
            String forValue = getRemoteAddress(remoteAddress);
            forwarded.put("for", forValue);
        }

        updated.add(FORWARDED_HEADER, forwarded.toHeaderValue());

        return updated;
    }

    private static String getRemoteAddress(InetSocketAddress remoteAddress) {
        String forValue;
        if (remoteAddress.isUnresolved()) {
            forValue = remoteAddress.getHostName();
        }
        else {
            InetAddress address = remoteAddress.getAddress();
            forValue = remoteAddress.getAddress().getHostAddress();
            if (address instanceof Inet6Address) {
                forValue = "[" + forValue + "]";
            }
        }
        int port = remoteAddress.getPort();
        if (port >= 0) {
            forValue = forValue + ":" + port;
        }
        return forValue;
    }

    static class Forwarded {

        private static final char EQUALS = '=';

        private static final char SEMICOLON = ';';

        private final Map<String, String> values;

        Forwarded() {
            this.values = new HashMap<>();
        }

        Forwarded(Map<String, String> values) {
            this.values = values;
        }

        public Forwarded put(String key, String value) {
            this.values.put(key, quoteIfNeeded(value));
            return this;
        }

        private String quoteIfNeeded(String s) {
            if (s != null && s.contains(":")) { // TODO: broaded quote
                return "\"" + s + "\"";
            }
            return s;
        }

        public String get(String key) {
            return this.values.get(key);
        }

        @Override
        public String toString() {
            return "Forwarded{" + "values=" + this.values + '}';
        }

        public String toHeaderValue() {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> entry : this.values.entrySet()) {
                if (!builder.isEmpty()) {
                    builder.append(SEMICOLON);
                }
                builder.append(entry.getKey()).append(EQUALS).append(entry.getValue());
            }
            return builder.toString();
        }

    }

}
