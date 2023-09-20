package com.apzda.cloud.gsvc.config;

import lombok.Data;

/**
 * @author fengz
 */
@Data
public class Config {

    private String loginPage;

    private String logoutPath;

    private String homePage;

    private boolean acceptLiteralFieldNames = true;

    private boolean properUnsignedNumberSerialization = true;

    private boolean serializeLongsAsString = true;

}
