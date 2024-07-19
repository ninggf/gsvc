package com.apzda.cloud.boot.aop;

import com.apzda.cloud.boot.TestApp;
import com.apzda.cloud.boot.autoconfig.GsvcBootAutoConfiguration;
import com.apzda.cloud.boot.controller.TestController;
import com.apzda.cloud.boot.entity.Role;
import com.apzda.cloud.boot.mapper.RoleMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private TestController testController;

    @BeforeEach
    void createEntity() {
        {
            var role = new Role();
            role.setRid("1");
            role.setName("1");
            role.setDd(0.25d);
            roleMapper.insert(role);

            role = new Role();
            role.setRid("2");
            role.setName("2");
            role.setDd(0.55d);
            roleMapper.insert(role);
        }
    }

    @Test
    void testResponse() {
        val testVoResponse = testController.testVoResponse("1");
        val node = objectMapper.convertValue(testVoResponse, JsonNode.class);

        assertThat(node.get("data").get("name").asText()).isEqualTo("test = 1");
        assertThat(node.get("data").get("statusLabel").asText()).isEqualTo("status1");
        assertThat(node.get("data").get("status2Label").asText()).isEqualTo("status2");
        assertThat(node.get("data").get("status3Label").asText()).isEqualTo("T3");
    }

}
