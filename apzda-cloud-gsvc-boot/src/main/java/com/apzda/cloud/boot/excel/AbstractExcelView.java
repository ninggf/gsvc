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
package com.apzda.cloud.boot.excel;

import cn.hutool.core.date.DateUtil;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.view.AbstractView;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public abstract class AbstractExcelView extends AbstractView {

    public static final String FILE_NAME = "fileName";

    @Override
    protected void renderMergedOutputModel(@Nonnull Map<String, Object> model, @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response) throws Exception {
        String codedFileName = "excel-" + DateUtil.newSimpleFormat("yyyyMMdd_HHmmss");
        if (model.containsKey(FILE_NAME) && StringUtils.isNotBlank(model.get(FILE_NAME).toString())) {
            codedFileName = (String) model.get(FILE_NAME);
        }
        codedFileName = URLEncoder.encode(codedFileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20") + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + codedFileName);

        doExport(model, request, response);
    }

    protected abstract void doExport(@Nonnull Map<String, Object> model, @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response) throws IOException;

}
