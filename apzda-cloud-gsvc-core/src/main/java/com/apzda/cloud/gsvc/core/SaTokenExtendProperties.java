package com.apzda.cloud.gsvc.core;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

/**
 * @author fengz
 */
@Data
@Validated
@ConfigurationProperties("sa-token.auth")
public class SaTokenExtendProperties {

    private URI loginUrl;

}
