package com.apzda.cloud.gsvc.config;

/**
 * @author fengz
 */
public class Config {

    private String loginPage;

    private String logoutPath;

    private String homePage;

    private boolean acceptLiteralFieldNames = true;

    private boolean properUnsignedNumberSerialization = true;

    private boolean serializeLongsAsString = true;

    public String getLoginPage() {
        return loginPage;
    }

    public void setLoginPage(String loginPage) {
        this.loginPage = loginPage;
    }

    public String getLogoutPath() {
        return logoutPath;
    }

    public void setLogoutPath(String logoutPath) {
        this.logoutPath = logoutPath;
    }

    public String getHomePage() {
        return homePage;
    }

    public void setHomePage(String homePage) {
        this.homePage = homePage;
    }

    public boolean isAcceptLiteralFieldNames() {
        return acceptLiteralFieldNames;
    }

    public void setAcceptLiteralFieldNames(boolean acceptLiteralFieldNames) {
        this.acceptLiteralFieldNames = acceptLiteralFieldNames;
    }

    public boolean isProperUnsignedNumberSerialization() {
        return properUnsignedNumberSerialization;
    }

    public void setProperUnsignedNumberSerialization(boolean properUnsignedNumberSerialization) {
        this.properUnsignedNumberSerialization = properUnsignedNumberSerialization;
    }

    public boolean isSerializeLongsAsString() {
        return serializeLongsAsString;
    }

    public void setSerializeLongsAsString(boolean serializeLongsAsString) {
        this.serializeLongsAsString = serializeLongsAsString;
    }

}
