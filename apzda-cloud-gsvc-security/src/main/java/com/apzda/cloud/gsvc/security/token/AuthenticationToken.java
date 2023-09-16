/*
 * This file is part of gsvc created at 2023/9/13 by ningGf.
 */
package com.apzda.cloud.gsvc.security.token;

import com.apzda.cloud.gsvc.security.JwtToken;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * @author fengz windywany@gmail.com
 **/
public abstract class AuthenticationToken extends AbstractAuthenticationToken {

    protected JwtToken jwtToken;

    public JwtToken getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(JwtToken jwtToken) {
        this.jwtToken = jwtToken;
    }

    /**
     * Creates a token with the supplied array of authorities.
     * @param authorities the collection of <tt>GrantedAuthority</tt>s for the principal
     * represented by this authentication object.
     */
    public AuthenticationToken(Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
    }

    public static class AnonymousToken extends AuthenticationToken {

        protected UserDetails anonymous;

        public AnonymousToken() {
            super(List.of(new SimpleGrantedAuthority("ANONYMOUS")));
        }

        public AnonymousToken(Collection<? extends GrantedAuthority> authorities) {
            super(authorities);
            this.anonymous = User.withUsername("anonymous").password("").authorities(authorities).build();
        }

        @Override
        public Object getCredentials() {
            return "";
        }

        @Override
        public Object getPrincipal() {
            return this.anonymous;
        }

    }

}
