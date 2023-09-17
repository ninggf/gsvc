package com.apzda.cloud.gsvc.dto;

import lombok.Builder;
import lombok.Data;

/**
 * @author fengz
 */
@Data
@Builder
public class CurrentUser {

    private String uid;

    private String device;

    private String deviceId;

}
