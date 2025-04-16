package com.apzda.cloud.mybatis.plugin;

import com.apzda.cloud.gsvc.config.ServiceConfigProperties;
import com.apzda.cloud.mybatis.TestApp;
import com.apzda.cloud.mybatis.autoconfigure.MyBatisPlusAutoConfiguration;
import com.apzda.cloud.mybatis.mapper.RoleMapper;
import com.apzda.cloud.mybatis.service.IRoleService;
import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author ninggf
 * @version 1.0.0
 * @since 2025/04/16
 */
@MybatisPlusTest
@ContextConfiguration(classes = TestApp.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration({ MyBatisPlusAutoConfiguration.class })
@ComponentScan("com.apzda.cloud.mybatis.service")
@Sql(value = "classpath:/init.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@EnableConfigurationProperties(ServiceConfigProperties.class)
class OptimisticLockerInnerInterceptorTest {

    @MockitoBean
    private Clock clock;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private IRoleService roleService;

    @Test
    @Sql("classpath:/dml.sql")
    void optimisticLockerInnerInterceptorShouldWork() {
        // given
        val role = roleMapper.selectById(1);
        val role2 = roleMapper.selectById(1);
        val role3 = roleMapper.selectById(1);
        // when
        role.setName("t1");
        roleMapper.updateById(role);

        role2.setName("t2");
        val rst = roleMapper.updateById(role2);
        assertThat(rst).isEqualTo(0);

        role3.setName("t3");
        // Exception?
        assertThat(roleService.updateById(role3)).isFalse();
    }

}
