package com.apzda.cloud.gsvc.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author fengz
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalConfig {

    /**
     * 临时文件路径
     */
    private String tmpPath;

    private boolean acceptLiteralFieldNames;

    private boolean properUnsignedNumberSerialization;

    private boolean serializeLongsAsString;

}
