package com.apzda.cloud.mybatis;

import com.apzda.cloud.gsvc.context.CurrentUserProvider;
import com.apzda.cloud.gsvc.context.TenantManager;
import com.apzda.cloud.gsvc.dto.CurrentUser;
import jakarta.annotation.Nonnull;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TestApp {

    @Bean
    TenantManager<String> tenantManager() {
        return new TenantManager<>() {
            @Override
            @Nonnull
            protected String[] getTenantIds() {
                return new String[] { "123456789" };
            }
        };
    }

    @Bean
    CurrentUserProvider currentUserProvider() {
        return new CurrentUserProvider() {
            @Override
            protected CurrentUser currentUser() {
                return CurrentUser.builder().id("1").uid("1").build();
            }
        };
    }

}
