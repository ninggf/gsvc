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
package com.apzda.cloud.gsvc.jpa.utils;

import com.apzda.cloud.gsvc.ext.GsvcExt;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.stream.Collectors;

/**
 * @author fengz (windywany@gmail.com)
 * @version 3.4.2
 * @since 3.4.2
 **/
public class PagerUtils extends PageRequest {

    public static final int DEFAULT_PAGE_SIZE = 30;

    /**
     * Creates a new {@link PageRequest} with sort parameters applied.
     * @param pageNumber zero-based page number, must not be negative.
     * @param pageSize the size of the page to be returned, must be greater than 0.
     * @param sort must not be {@literal null}, use {@link Sort#unsorted()} instead.
     */
    protected PagerUtils(int pageNumber, int pageSize, Sort sort) {
        super(pageNumber, pageSize, sort);
    }

    @Nonnull
    public static PageRequest of(@Nullable GsvcExt.Pager pager) {
        if (pager == null) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE);
        }
        int pageNumber = pager.getPageNumber();
        int pageSize = pager.hasPageSize() ? pager.getPageSize() : DEFAULT_PAGE_SIZE;

        if (pager.hasSort()) {
            val sort = pager.getSort();
            val orders = sort.getOrderList().stream().map(order -> {
                if (order.getDirection() == GsvcExt.Sorter.Direction.ASC) {
                    return Sort.Order.asc(order.getField());
                }
                else {
                    return Sort.Order.desc(order.getField());
                }
            }).collect(Collectors.toList());

            return PageRequest.of(pageNumber, pageSize, Sort.by(orders));
        }
        return PageRequest.of(pageNumber, pageSize);
    }

    @Nonnull
    public static GsvcExt.Pager to(@Nullable Pageable pager) {
        val builder = GsvcExt.Pager.newBuilder();
        if (pager == null) {
            builder.setPageNumber(0);
            builder.setPageSize(DEFAULT_PAGE_SIZE);
        }
        else {
            builder.setPageNumber(pager.getPageNumber());
            if (pager.getPageSize() > 0) {
                builder.setPageSize(pager.getPageSize());
            }
            if (pager.getOffset() > 0) {
                builder.setOffset(pager.getOffset());
            }
            builder.setSort(convertSort(pager.getSort()));
        }

        return builder.build();
    }

    @Nonnull
    public static GsvcExt.PageInfo of(@Nonnull Page<?> page) {
        val builder = GsvcExt.PageInfo.newBuilder();
        builder.setPageNumber(page.getNumber());
        builder.setTotalPages(page.getTotalPages());
        builder.setTotalElements(page.getTotalElements());
        builder.setNumberOfElements(page.getNumberOfElements());
        builder.setPageSize(page.getSize());
        builder.setFirst(page.isFirst());
        builder.setLast(page.isLast());
        builder.setSort(convertSort(page.getSort()));
        return builder.build();
    }

    @Nonnull
    public static GsvcExt.Sorter.Builder convertSort(@Nullable Sort sort) {
        val sb = GsvcExt.Sorter.newBuilder();
        if (sort != null) {
            sort.stream().forEach(order -> {
                val ob = GsvcExt.Sorter.Order.newBuilder().setField(order.getProperty());
                if (order.getDirection() == Sort.Direction.ASC) {
                    sb.addOrder(ob.setDirection(GsvcExt.Sorter.Direction.ASC));
                }
                else {
                    sb.addOrder(ob.setDirection(GsvcExt.Sorter.Direction.DESC));
                }
            });
        }
        return sb;
    }

}
