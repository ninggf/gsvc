/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.dto;

import org.apache.commons.lang3.StringUtils;

/**
 * @author fengz windywany@gmail.com
 **/
public enum MessageType {

    TOAST, ALERT, NOTIFY, NONE;

    public static MessageType fromString(String text) {
        if ("toast".equalsIgnoreCase(text)) {
            return TOAST;
        }
        else if ("alert".equalsIgnoreCase(text)) {
            return ALERT;
        }
        else if ("notify".equalsIgnoreCase(text)) {
            return NOTIFY;
        }
        else if (StringUtils.isNoneBlank(text)) {
            return NOTIFY;
        }
        return NONE;
    }

}
