package com.apzda.cloud.gsvc.security.resolver;

import com.apzda.cloud.gsvc.client.IServiceCaller;
import com.apzda.cloud.gsvc.context.CurrentUserProvider;
import com.apzda.cloud.gsvc.context.TenantManager;
import com.apzda.cloud.gsvc.dto.CurrentUser;
import com.apzda.cloud.gsvc.security.authorization.AsteriskPermissionEvaluator;
import com.apzda.cloud.gsvc.security.authorization.AuthorizationLogicCustomizer;
import com.apzda.cloud.gsvc.security.authorization.PermissionChecker;
import com.apzda.cloud.gsvc.security.config.SecurityConfigProperties;
import com.apzda.cloud.gsvc.security.dto.CardDto;
import com.apzda.cloud.gsvc.security.dto.StaffDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@WebMvcTest(controllers = TestController.class)
@ContextConfiguration(classes = CurrentUserParamResolverTest.WebMvcConfigure.class)
@Import({ CurrentUserParamResolverTest.WebMvcConfigure.class, TestController.class })
@ActiveProfiles("test")
@TestPropertySource(properties = "apzda.cloud.security.allowed-devices=pc,ios")
class CurrentUserParamResolverTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private IServiceCaller serviceCaller;

    @Autowired
    private SecurityConfigProperties properties;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser("gsvc")
    void ok() throws Exception {
        mvc.perform(get("/test/ok")).andExpect(status().isOk()).andExpect(content().string("gsvc"));
    }

    @Test
    void ok1() throws Exception {
        mvc.perform(get("/test/ok").accept(MediaType.TEXT_HTML_VALUE))
            .andExpect(status().is(302))
            .andExpect(header().exists("Location"));
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

    @Test
    @WithMockUser("gsvc")
    void isMine() throws Exception {
        val card = new CardDto();
        card.setCreatedBy("gsvc");
        mvc.perform(post("/test/card").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(card))
            .accept(MediaType.TEXT_HTML)).andExpect(status().isOk()).andExpect(content().string("gsvc"));
    }

    @Test
    @WithMockUser("gsvc")
    void isMine_withStringId() throws Exception {
        mvc.perform(get("/test/card/gsvc").accept(MediaType.TEXT_HTML))
            .andExpect(status().isOk())
            .andExpect(content().string("gsvc"));
    }

    @Test
    @WithMockUser("gsvc")
    void isOwned() throws Exception {
        val staffDto = new StaffDto();
        staffDto.setTenantId(1L);
        mvc.perform(post("/test/staff").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(staffDto))
            .accept(MediaType.TEXT_HTML)).andExpect(status().isOk()).andExpect(content().string("1"));

        staffDto.setTenantId(2L);
        mvc.perform(post("/test/staff").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(staffDto))
            .accept(MediaType.TEXT_HTML)).andExpect(status().isOk()).andExpect(content().string("2"));

        staffDto.setTenantId(3L);
        mvc.perform(post("/test/staff").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(staffDto))
            .accept(MediaType.TEXT_HTML)).andExpect(status().isOk()).andExpect(content().string("3"));
    }

    @Test
    @WithMockUser("gsvc")
    void isOwned_withStringId() throws Exception {
        mvc.perform(get("/test/staff/1").accept(MediaType.TEXT_HTML))
            .andExpect(status().isOk())
            .andExpect(content().string("1"));
        mvc.perform(get("/test/staff/2").accept(MediaType.TEXT_HTML))
            .andExpect(status().isOk())
            .andExpect(content().string("2"));
        mvc.perform(get("/test/staff/3").accept(MediaType.TEXT_HTML))
            .andExpect(status().isOk())
            .andExpect(content().string("3"));
    }

    @Test
    @WithMockUser("gsvc")
    void isNotMine() throws Exception {
        val card = new CardDto();
        card.setCreatedBy("gsvcx");
        mvc.perform(post("/test/card").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(card))
            .accept(MediaType.TEXT_HTML)).andExpect(status().is(403));
    }

    @Test
    @WithMockUser("gsvc")
    void isNotMine_withStringId() throws Exception {
        mvc.perform(get("/test/card/gsvcx").accept(MediaType.TEXT_HTML)).andExpect(status().is(403));
    }

    @Test
    @WithMockUser("gsvc")
    void isNotOwned() throws Exception {
        val staffDto = new StaffDto();
        staffDto.setTenantId(5L);
        mvc.perform(post("/test/staff").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(staffDto))
            .accept(MediaType.TEXT_HTML)).andExpect(status().is(403));
    }

    @Test
    @WithMockUser("gsvc")
    void isNotOwned_withStringId() throws Exception {
        mvc.perform(get("/test/staff/4").accept(MediaType.TEXT_HTML)).andExpect(status().is(403));
    }

    @Test
    @WithMockUser(value = "gsvc", authorities = "view,new:gsvc.user.*")
    void iCan_should_ok() throws Exception {
        mvc.perform(get("/test/authority").accept(MediaType.TEXT_HTML)).andExpect(status().isOk());
        mvc.perform(get("/test/ican/1").accept(MediaType.TEXT_HTML)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(value = "gsvc", authorities = "view:gsvc.user.1,2,3")
    void iCan_with_id_should_ok() throws Exception {
        mvc.perform(get("/test/authority").accept(MediaType.TEXT_HTML)).andExpect(status().is(403));
        mvc.perform(get("/test/ican/3").accept(MediaType.TEXT_HTML)).andExpect(status().isOk());
        mvc.perform(get("/test/ican/4").accept(MediaType.TEXT_HTML)).andExpect(status().is(403));
    }

    @Configuration
    @EnableConfigurationProperties(SecurityConfigProperties.class)
    @EnableMethodSecurity
    static class WebMvcConfigure implements WebMvcConfigurer {

        @Override
        public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new CurrentUserParamResolver());
        }

        @Bean
        PermissionEvaluator permissionEvaluator(ObjectProvider<PermissionChecker> permissionEvaluatorProvider) {
            return new AsteriskPermissionEvaluator(permissionEvaluatorProvider);
        }

        @Bean
        AuthorizationLogicCustomizer authz(PermissionEvaluator permissionEvaluator) {
            return new AuthorizationLogicCustomizer(permissionEvaluator);
        }

        @Bean
        TenantManager<String> tenantManager() {
            return new TenantManager<>() {
                @Override
                @NonNull
                protected String[] getTenantIds() {
                    return new String[] { "1", "2", "3" };
                }
            };
        }

        @Bean
        CurrentUserProvider currentUserProvider() {
            return new CurrentUserProvider() {
                @Override
                protected CurrentUser currentUser() {
                    val context = SecurityContextHolder.getContext();
                    val authentication = context.getAuthentication();
                    if (authentication != null) {
                        return CurrentUser.builder()
                            .uid(((UserDetails) authentication.getPrincipal()).getUsername())
                            .build();
                    }
                    return null;
                }
            };
        }

    }

}
