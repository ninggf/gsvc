package com.apzda.cloud.boot.aop;

import com.apzda.cloud.boot.TestApp;
import com.apzda.cloud.boot.autoconfig.GsvcBootAutoConfiguration;
import com.apzda.cloud.boot.controller.TestController;
import com.apzda.cloud.boot.dict.TransformUtils;
import com.apzda.cloud.boot.mapper.DictItemMapper;
import com.apzda.cloud.boot.transformer.Upper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @MockitoSpyBean
    private DictItemMapper dictItemMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestController testController;

    @Test
    void testResponse() {
        val testVoResponse = testController.testVoResponse("1");
        val node = objectMapper.convertValue(testVoResponse, JsonNode.class);
        val result = node.get("data");
        assertThat(result.get("name").asText()).isEqualTo("test = 1");
        assertThat(result.get("statusText").asText()).isEqualTo("status1");
        assertThat(result.get("status2Text").asText()).isEqualTo("status2");
        assertThat(result.get("status3Text").asText()).isEqualTo("T3");
        assertThat(result.get("phone").asText()).isEqualTo("131****6666");
        assertThat(result.get("phone1").asText()).isEqualTo("131****6666");
        assertThat(result.get("phone2").asText()).isEqualTo("13166666666");
        assertThat(result.get("phone2Text").asText()).isEqualTo("13166666666-test");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getGetUserList() {
        // given
        val upper = spy(Upper.class);
        try (val mocked = Mockito.mockStatic(TransformUtils.class)) {
            mocked.when(() -> {
                TransformUtils.getTransformer(Upper.class);
            }).thenReturn(upper);

            // when
            val users = testController.getUserList();
            // then
            val data = objectMapper.convertValue(users.getData(), List.class);
            assertThat(data.size()).isEqualTo(3);
            assertThat(data.get(0)).isInstanceOf(Map.class);
            val user1 = (Map<String, String>) data.get(0);
            assertThat(user1.get("nameText")).isEqualTo("U1");
            assertThat(user1.get("name1Text")).isEqualTo("U1");
            assertThat(user1.get("rolesText")).isEqualTo("r1");
            assertThat(user1.get("typeText")).isEqualTo("Test1");
            val user2 = (Map<String, String>) data.get(1);
            assertThat(user2.get("nameText")).isEqualTo("U2");
            assertThat(user2.get("name1Text")).isEqualTo("U1");
            assertThat(user2.get("rolesText")).isEqualTo("r2");
            assertThat(user2.get("typeText")).isEqualTo("test3");

            verify(dictItemMapper, times(2)).getDictLabel(any(), any(), any(), any());
            verify(dictItemMapper, times(1)).getDictLabel(any(), any(), any(), any(), any());
            verify(upper, times(3)).transform(any());
        }
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
