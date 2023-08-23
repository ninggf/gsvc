package com.apzda.cloud.gsvc.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author fengz
 */
@Data
@Accessors(chain = true)
public class CurrentUser {
    private String uid;
}
