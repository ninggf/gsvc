package com.apzda.cloud.gsvc.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodConfig {

    @DurationUnit(ChronoUnit.MILLIS)
    private Duration timeout = Duration.ZERO;

    @DurationUnit(ChronoUnit.MILLIS)
    private Duration connectTimeout = Duration.ZERO;

    @DurationUnit(ChronoUnit.MILLIS)
    private Duration readTimeout = Duration.ZERO;

    @DurationUnit(ChronoUnit.MILLIS)
    private Duration uploadTimeout = Duration.ZERO;

    private final List<String> plugins = new ArrayList<>();

}
