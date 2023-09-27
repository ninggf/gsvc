package com.apzda.cloud.gsvc.config;

import lombok.Data;
import org.springframework.core.style.ToStringCreator;

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

    @Override
    public String toString() {
        return new ToStringCreator(this).append("Login", loginPage)
            .append("Logout", logoutPath)
            .append("home", homePage)
            .append("acceptLiteralFieldNames", acceptLiteralFieldNames)
            .append("properUnsignedNumberSerialization", properUnsignedNumberSerialization)
            .append("serializeLongsAsString", serializeLongsAsString)
            .toString();
    }

}
