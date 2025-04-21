/*
 * Copyright (C) 2023-2025 Fengz Ning (windywany@gmail.com)
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
package com.apzda.cloud.gsvc.dto;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * @author ninggf
 * @version 3.4.3
 */
@Data
public class PageResult<T> {

    /**
     * 当前页
     */
    private long current = 1;

    /**
     * 总页数
     */
    private long pages;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 每页条数
     */
    private long size;

    /**
     * 内容体
     */
    private List<T> results = Collections.emptyList();

    public PageResult() {
    }

    public PageResult(long current, long totalPage, long totalRecord, long size, List<T> results) {
        this.current = current;
        this.pages = totalPage;
        this.size = size;
        this.total = totalRecord;
        this.results = results;
    }

    public PageResult(List<T> results, long totalRecord) {
        this.total = totalRecord;
        this.results = results;
    }

}
