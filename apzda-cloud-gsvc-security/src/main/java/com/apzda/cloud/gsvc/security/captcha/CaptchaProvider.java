package com.apzda.cloud.gsvc.security.captcha;

import org.springframework.security.core.Authentication;

/**
 * @author fengz
 */
public interface CaptchaProvider {

    boolean valid(Authentication captchaToken, boolean removeOnFailure);

    Captcha generate();

}
