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
package com.apzda.cloud.gsvc.resolver;

import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.apzda.cloud.gsvc.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Slf4j
public class PagerResolver implements HandlerMethodArgumentResolver {

    private final String pageNumber;

    private final String pageSize;

    private final String pageSorts;

    public PagerResolver(ServiceConfigProperties.Config config) {
        this.pageNumber = org.apache.commons.lang3.StringUtils.defaultIfBlank(config.getPageNumber(), "pageNumber");
        this.pageSize = org.apache.commons.lang3.StringUtils.defaultIfBlank(config.getPageSize(), "pageSize");
        this.pageSorts = org.apache.commons.lang3.StringUtils.defaultIfBlank(config.getPageSorts(), "pageSorts");
    }

    @Override
    public boolean supportsParameter(@Nonnull MethodParameter parameter) {
        return parameter.getParameter().getType().isAssignableFrom(GsvcExt.Pager.class);
    }

    @Override
    public Object resolveArgument(@Nonnull MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
            @Nonnull NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        val parameterMap = webRequest.getParameterMap();
        val pz = parameterMap.getOrDefault(this.pageSize, new String[] { "10" });
        val pn = parameterMap.getOrDefault(this.pageNumber, new String[] { "0" });
        val builder = GsvcExt.Pager.newBuilder()
            .setPageNumber(Integer.parseInt(pn[0]))
            .setPageSize(Integer.parseInt(pz[0]));
        val sortFields = parameterMap.getOrDefault(this.pageSorts, new String[] {});
        val sorter = GsvcExt.Sorter.newBuilder();

        for (String sortField : sortFields) {
            val sorts = sortField.split(",");
            for (String sort : sorts) {
                val fs = sort.split("\\|");
                var so = GsvcExt.Sorter.Direction.ASC;
                if (fs.length > 1) {
                    so = GsvcExt.Sorter.Direction.valueOf(fs[1].toUpperCase());
                }
                sorter.addOrder(GsvcExt.Sorter.Order.newBuilder()
                    .setField(StringUtils.camelToUnderline(fs[0]))
                    .setDirection(so)
                    .build());
            }
        }

        if (sorter.getOrderCount() > 0) {
            builder.setSort(sorter);
        }
        return builder.build();
    }

}
