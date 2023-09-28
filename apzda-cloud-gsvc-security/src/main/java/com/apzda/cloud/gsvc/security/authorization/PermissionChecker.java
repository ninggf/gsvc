package com.apzda.cloud.gsvc.security.authorization;

import org.springframework.security.core.Authentication;

import java.io.Serializable;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
public interface PermissionChecker {

    Boolean check(Authentication authentication, Object obj, String permission);

    Boolean check(Authentication authentication, Serializable targetId, String targetType, String permission);

    boolean supports(Class<?> objClazz);

    boolean supports(String targetType);

}
