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
package com.apzda.cloud.gsvc.domain;

import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
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

    /**
     * 将请求中的分页参数转为MyBatis Plus分页器{@link IPage}。
     * @param pager 分页参数
     * @return MyBatis Plus 分页器
     */
    @Nonnull
    public static <T> IPage<T> of(@Nullable GsvcExt.Pager pager, Class<T> tClass) {
        val page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<T>();
        if (pager != null) {
            int pageSize = pager.hasPageSize() ? pager.getPageSize() : DEFAULT_PAGE_SIZE;

            page.setCurrent(pager.getPageNumber() + 1);
            page.setSize(pageSize);
            if (pager.hasSort()) {
                val sort = pager.getSort();
                if (sort.getOrderCount() > 0) {
                    var orders = new ArrayList<OrderItem>();
                    for (GsvcExt.Sorter.Order order : sort.getOrderList()) {
                        val o = new OrderItem();
                        o.setAsc(order.getDirection() == GsvcExt.Sorter.Direction.ASC);
                        o.setColumn(order.getField());
                        orders.add(o);
                    }
                    page.setOrders(orders);
                }
            }
        }
        else {
            page.setSize(DEFAULT_PAGE_SIZE);
        }
        return page;
    }

    /**
     * 将MyBatis Plus分页器{@link IPage}转换为Gsvc请求分页器{@link GsvcExt.Pager}。
     * @param page MyBatis Plus分页器
     * @return Gsvc请求分页器实例.
     */
    @Nonnull
    public static GsvcExt.Pager to(@Nullable IPage<?> page) {
        val builder = GsvcExt.Pager.newBuilder();
        if (page == null) {
            builder.setPageNumber(0);
            builder.setPageSize(DEFAULT_PAGE_SIZE);
        }
        else {
            builder.setPageNumber(Math.max(0, (int) page.getCurrent() - 1));
            if (page.getSize() > 0) {
                builder.setPageSize((int) page.getSize());
            }
            if (page.offset() > 0) {
                builder.setOffset(page.offset());
            }
            builder.setSort(convertSort(page.orders()));
        }

        return builder.build();
    }

    /**
     * 将MyBatis Plus分页结果{@link IPage}转换为Gsvc分页结果{@link GsvcExt.PageInfo}。
     * @param page MyBatis Plus分页结果
     * @return Gsvc分页结果
     */
    @Nonnull
    public static GsvcExt.PageInfo of(@Nonnull IPage<?> page) {
        val current = page.getCurrent();
        val pages = page.getPages();
        val builder = GsvcExt.PageInfo.newBuilder();
        builder.setPageNumber(Math.max(0, (int) (current - 1)));
        builder.setTotalPages((int) pages);
        builder.setTotalElements(page.getTotal());
        builder.setNumberOfElements(page.getRecords().size());
        builder.setPageSize((int) page.getSize());
        builder.setFirst(current == 1);
        builder.setLast(current == pages);
        builder.setSort(convertSort(page.orders()));

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

    @Nonnull
    public static GsvcExt.Sorter.Builder convertSort(@Nullable List<OrderItem> orders) {
        val builder = GsvcExt.Sorter.newBuilder();

        if (!CollectionUtils.isEmpty(orders)) {
            for (OrderItem order : orders) {
                val ob = GsvcExt.Sorter.Order.newBuilder().setField(order.getColumn());
                if (order.isAsc()) {
                    builder.addOrder(ob.setDirection(GsvcExt.Sorter.Direction.ASC));
                }
                else {
                    builder.addOrder(ob.setDirection(GsvcExt.Sorter.Direction.DESC));
                }
            }
        }

        return builder;
    }

}
