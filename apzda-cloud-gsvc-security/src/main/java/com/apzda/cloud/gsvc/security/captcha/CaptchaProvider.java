package com.apzda.cloud.gsvc.security.captcha;

import org.springframework.web.servlet.function.ServerRequest;

/**
 * @author fengz
 */
public interface CaptchaProvider<T extends CaptchaWidget> {

    String getId();

    boolean valid(String captchaId, ServerRequest request, boolean removeOnFailure);

    T create(String captchaId);

    boolean supports(String id);

}
