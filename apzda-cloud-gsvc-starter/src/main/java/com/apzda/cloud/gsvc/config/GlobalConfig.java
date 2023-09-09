package com.apzda.cloud.gsvc.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalConfig {

    /**
     * 临时文件路径
     */
    private String tmpPath;

    @DurationUnit(ChronoUnit.MILLIS)
    private Duration uploadTimeout = Duration.ZERO;

    private boolean acceptLiteralFieldNames;

    private boolean properUnsignedNumberSerialization;

    private boolean serializeLongsAsString;

}
