package com.apzda.cloud.gsvc.security.userdetails;

public enum UserFlag {

    isAccountNonExpired(0b0000_0001), isAccountNonLocked(0b0000_0010), isCredentialsNonExpired(0b0000_0100),
    isEnabled(0b0000_1000);

    public final int flag;

    UserFlag(int flag) {
        this.flag = flag;
    }

}
