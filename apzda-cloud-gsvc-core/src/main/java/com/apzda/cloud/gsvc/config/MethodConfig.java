package com.apzda.cloud.gsvc.config;

import lombok.Data;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Data
public class MethodConfig {

    private final List<String> plugins = new ArrayList<>();

    @DurationUnit(ChronoUnit.MILLIS)
    private Duration timeout = Duration.ZERO;

    /**
     * the timeout of read request body
     */
    @DurationUnit(ChronoUnit.MILLIS)
    private Duration readTimeout = Duration.ZERO;

}
