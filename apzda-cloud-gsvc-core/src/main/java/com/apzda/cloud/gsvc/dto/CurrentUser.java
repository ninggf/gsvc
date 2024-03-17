package com.apzda.cloud.gsvc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Map;

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

    private Map<String, String> meta;

    public Map<String, String> getMeta() {
        if (CollectionUtils.isEmpty(meta)) {
            return Collections.emptyMap();
        }
        return meta;
    }
}
