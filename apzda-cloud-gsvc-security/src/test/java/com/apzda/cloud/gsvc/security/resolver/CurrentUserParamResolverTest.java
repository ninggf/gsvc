package com.apzda.cloud.gsvc.security.resolver;

import com.apzda.cloud.gsvc.client.IServiceCaller;
import com.apzda.cloud.gsvc.security.config.SecurityConfigProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@WebMvcTest(controllers = TestController.class)
@ContextConfiguration(classes = CurrentUserParamResolverTest.WebMvcConfigure.class)
@Import({
    CurrentUserParamResolverTest.WebMvcConfigure.class, TestController.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = "apzda.cloud.security.allowed-devices=pc,ios")
class CurrentUserParamResolverTest {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private IServiceCaller serviceCaller;

    @Autowired
    private SecurityConfigProperties properties;

    @Test
    @WithMockUser("gsvc")
    void ok() throws Exception {
        mvc.perform(get("/test/ok")).andExpect(status().isOk()).andExpect(content().string("gsvc"));
    }

    @Test
    void ok1() throws Exception {
        mvc.perform(get("/test/ok").accept(MediaType.TEXT_HTML_VALUE)).andExpect(status().is(302)).andExpect(header().exists("Location"));
    }

    @Test
    void deviceIsNotAllowed() {
        assertThat(properties.deviceIsAllowed("pc")).isTrue();
        assertThat(properties.deviceIsAllowed("ios")).isTrue();
        assertThat(properties.deviceIsAllowed("android")).isFalse();
        assertThat(properties.deviceIsAllowed("")).isFalse();
        assertThat(properties.deviceIsAllowed(null)).isFalse();

        properties.setAllowedDevices(null);
        assertThat(properties.deviceIsAllowed("pc")).isTrue();
        assertThat(properties.deviceIsAllowed("ios")).isTrue();
        assertThat(properties.deviceIsAllowed("android")).isTrue();
        assertThat(properties.deviceIsAllowed("")).isFalse();
        assertThat(properties.deviceIsAllowed(null)).isFalse();
    }

    @Configuration
    @EnableConfigurationProperties(SecurityConfigProperties.class)
    static class WebMvcConfigure implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new CurrentUserParamResolver());
        }
    }
}
