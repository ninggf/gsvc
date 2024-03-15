package com.apzda.cloud.gsvc.security.userdetails;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
class InMemoryUserDetailsMetaRepositoryTest {

    @Test
    void getMultiMetaData() {
        // given
        val user = User.withUsername("tester").password("123").authorities("ADMIN").build();
        val metaRepository = new InMemoryUserDetailsMetaRepository(UserDetails::getAuthorities, SimpleGrantedAuthority.class);
        // when
        metaRepository.getAuthorities(user);
        val authorities = metaRepository.getAuthorities(user);
        // then
        assertThat(authorities.size()).isEqualTo(1);
        val as = authorities.toArray(new org.springframework.security.core.GrantedAuthority[0]);
        assertThat(as[0].getAuthority()).isEqualTo("ADMIN");
    }
}
