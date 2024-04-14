/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
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
package com.apzda.cloud.gsvc.gtw;

import org.springframework.util.AntPathMatcher;

import java.util.HashSet;
import java.util.Set;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public abstract class RouteRegistry {

    public static final AntPathMatcher pathMatcher = new AntPathMatcher();

    static {
        pathMatcher.setCachePatterns(true);
    }

    private final static Set<String> routes = new HashSet<>();

    private final static Set<String> ignors = new HashSet<>() {
        {
            add("**/favicon.icon?");
            add("**/*.html?");
            add("/static/**");
        }
    };

    public static void register(String route) {
        routes.add(route);
    }

    public static String match(String route) {
        for (String r : routes) {
            if (pathMatcher.match(r, route)) {
                return r;
            }
        }
        return route;
    }

    public static boolean ignore(String route) {
        for (String r : ignors) {
            if (pathMatcher.match(r, route)) {
                return true;
            }
        }
        return false;
    }

}
