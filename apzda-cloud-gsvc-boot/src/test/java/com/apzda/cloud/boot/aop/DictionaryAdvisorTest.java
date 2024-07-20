package com.apzda.cloud.boot.aop;

import com.apzda.cloud.boot.TestApp;
import com.apzda.cloud.boot.autoconfig.GsvcBootAutoConfiguration;
import com.apzda.cloud.boot.controller.TestController;
import com.apzda.cloud.boot.mapper.DictItemMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@SpringBootTest
@ContextConfiguration(classes = TestApp.class)
@ImportAutoConfiguration(classes = { AopAutoConfiguration.class, GsvcBootAutoConfiguration.class })
@TestPropertySource(properties = { "logging.level.com.apzda.cloud=trace" })
@AutoConfigureMockMvc
@Sql("classpath:/schema.sql")
class DictionaryAdvisorTest {

    @SpyBean
    private DictItemMapper dictItemMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestController testController;

    @Test
    void testResponse() {
        val testVoResponse = testController.testVoResponse("1");
        val node = objectMapper.convertValue(testVoResponse, JsonNode.class);

        assertThat(node.get("data").get("name").asText()).isEqualTo("test = 1");
        assertThat(node.get("data").get("statusText").asText()).isEqualTo("status1");
        assertThat(node.get("data").get("status2Text").asText()).isEqualTo("status2");
        assertThat(node.get("data").get("status3Text").asText()).isEqualTo("T3");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getGetUserList() {
        // when
        val users = testController.getUserList();
        // then
        val data = objectMapper.convertValue(users.getData(), List.class);
        assertThat(data.size()).isEqualTo(3);
        assertThat(data.get(0)).isInstanceOf(Map.class);
        val user1 = (Map<String, String>) data.get(0);
        assertThat(user1.get("rolesText")).isEqualTo("r1");
        assertThat(user1.get("typeText")).isEqualTo("Test1");
        val user2 = (Map<String, String>) data.get(1);
        assertThat(user2.get("rolesText")).isEqualTo("r2");
        assertThat(user2.get("typeText")).isEqualTo("test3");

        verify(dictItemMapper, times(2)).getDictLabel(any(), any(), any(), any());
        verify(dictItemMapper, times(1)).getDictLabel(any(), any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getGetUserPage() {
        // when
        val users = testController.getUserPage();
        // then
        val data = objectMapper.convertValue(users.getData().getRecords(), List.class);
        assertThat(data.size()).isEqualTo(3);
        assertThat(data.get(0)).isInstanceOf(Map.class);
        val user1 = (Map<String, String>) data.get(0);
        assertThat(user1.get("rolesText")).isEqualTo("r1");
        assertThat(user1.get("typeText")).isEqualTo("Test1");
        val user2 = (Map<String, String>) data.get(1);
        assertThat(user2.get("rolesText")).isEqualTo("r2");
        assertThat(user2.get("typeText")).isEqualTo("test3");

        verify(dictItemMapper, times(2)).getDictLabel(any(), any(), any(), any());
        verify(dictItemMapper, times(1)).getDictLabel(any(), any(), any(), any(), any());
    }

}
