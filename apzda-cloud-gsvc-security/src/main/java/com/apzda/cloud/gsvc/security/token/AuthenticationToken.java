/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.token;

import com.apzda.cloud.gsvc.security.IUser;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * @author fengz windywany@gmail.com
 **/
public abstract class AuthenticationToken extends AbstractAuthenticationToken implements IUser {

    /**
     * Creates a token with the supplied array of authorities.
     * @param authorities the collection of <tt>GrantedAuthority</tt>s for the principal
     * represented by this authentication object.
     */
    public AuthenticationToken(Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
    }

}
