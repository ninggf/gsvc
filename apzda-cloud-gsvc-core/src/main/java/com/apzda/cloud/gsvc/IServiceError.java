package com.apzda.cloud.gsvc;

import com.apzda.cloud.gsvc.dto.MessageType;

/**
 * @author fengz
 */
public interface IServiceError {

    int code();

    String message();

    default MessageType type() {
        return null;
    }

}
