package com.apzda.cloud.gsvc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * @author fengz
 */
@Data
@Builder
@Schema(title = "Current User")
public class CurrentUser {

    private String uid;

    private String device;

    private String deviceId;

    private String os;

    private String osVer;

    private String app;

}
