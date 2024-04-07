package com.apzda.cloud.gsvc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * @author fengz
 */
@Data
@Builder
@Schema(title = "Current User")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrentUser implements Serializable {

    @Serial
    private static final long serialVersionUID = -8353034394933412487L;

    private String uid;

    private String device;

    private String deviceId;

    private String os;

    private String osVer;

    private String app;

    private String remoteAddress;

    private Map<String, String> meta;

    public Map<String, String> getMeta() {
        if (CollectionUtils.isEmpty(meta)) {
            return Collections.emptyMap();
        }
        return meta;
    }

}
