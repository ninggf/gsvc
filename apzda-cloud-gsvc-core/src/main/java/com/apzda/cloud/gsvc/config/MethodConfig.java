package com.apzda.cloud.gsvc.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MethodConfig {

    private final List<String> plugins = new ArrayList<>();

}
