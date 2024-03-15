/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.gsvc.security.userdetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.val;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@Data
public class CachedUserDetails implements UserDetails {
    @JsonIgnore
    Collection<? extends GrantedAuthority> authorities;
    private String password;
    private String username;
    boolean accountNonExpired;
    boolean accountNonLocked;
    boolean credentialsNonExpired;
    boolean enabled;

    public static CachedUserDetails from(UserDetails userDetails) {
        val ud = new CachedUserDetails();

        ud.authorities = userDetails.getAuthorities();
        ud.password = userDetails.getPassword();
        ud.username = userDetails.getUsername();
        ud.accountNonExpired = userDetails.isAccountNonExpired();
        ud.accountNonLocked = userDetails.isAccountNonLocked();
        ud.credentialsNonExpired = userDetails.isCredentialsNonExpired();
        ud.enabled = userDetails.isEnabled();

        return ud;
    }
}
