package com.apzda.cloud.gsvc.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;

/**
 * @author fengz
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalConfig {

    private URI loginPage;

    private URI logoutPath;

    private URI homePage;

    private boolean acceptLiteralFieldNames;

    private boolean properUnsignedNumberSerialization;

    private boolean serializeLongsAsString;

}
