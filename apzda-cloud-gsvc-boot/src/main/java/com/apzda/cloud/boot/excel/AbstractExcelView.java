package com.apzda.cloud.boot.excel;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.view.AbstractView;

public abstract class AbstractExcelView extends AbstractView {

    private static final String CONTENT_TYPE = "application/vnd.ms-excel";

    public static final String HSSF = ".xls";

    public static final String XSSF = ".xlsx";

    public AbstractExcelView() {
        this.setContentType("application/vnd.ms-excel");
    }

    protected boolean isIE(HttpServletRequest request) {
        return request.getHeader("USER-AGENT").toLowerCase().indexOf("msie") > 0
                || request.getHeader("USER-AGENT").toLowerCase().indexOf("rv:11.0") > 0;
    }

}
