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

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.RowWriteHandler;
import com.alibaba.excel.write.handler.WriteHandler;
import jakarta.annotation.Nonnull;
import lombok.val;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.apzda.cloud.boot.excel.AbstractExcelView.FILE_NAME;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public class XlsxView<T> extends ModelAndView implements CellWriteHandler, RowWriteHandler {

    private final List<T> rows;

    public XlsxView(@Nonnull String fileName) {
        this(fileName, new ArrayList<>());
    }

    public XlsxView(@Nonnull String fileName, @Nonnull List<T> rows) {
        this(fileName, fileName, rows);
    }

    public XlsxView(@Nonnull String fileName, @Nonnull String sheetName, @Nonnull List<T> rows) {
        this.rows = rows;
        WriteHandler handler = this;

        setView(new AbstractExcelView() {
            @Override
            protected void doExport(@Nonnull Map<String, Object> model,
                    @Nonnull jakarta.servlet.http.HttpServletRequest request,
                    @Nonnull jakarta.servlet.http.HttpServletResponse response) throws IOException {
                val data = getRows();
                Class<?> elementClz = null;
                if (!CollectionUtils.isEmpty(data)) {
                    elementClz = data.get(0).getClass();
                }

                EasyExcel.write(response.getOutputStream(), elementClz)
                    .registerWriteHandler(handler)
                    .sheet(sheetName)
                    .doWrite(data);
            }
        });

        addObject(FILE_NAME, fileName);
    }

    @Nonnull
    protected List<T> getRows() {
        return this.rows;
    }

}
