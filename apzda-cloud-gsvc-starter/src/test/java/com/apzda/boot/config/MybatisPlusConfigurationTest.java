package com.apzda.boot.config;

import com.apzda.TestApp;
import com.apzda.boot.MyBatisPlusConfig;
import com.apzda.boot.entity.Role;
import com.apzda.boot.entity.User;
import com.apzda.cloud.gsvc.autoconfigure.MyBatisPlusAutoConfiguration;
import com.apzda.module.test.abc.def.a.mapper.UserMapper;
import com.apzda.neti.test.mapper.RoleMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import jakarta.annotation.Resource;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created at 2023/7/7 13:45.
 *
 * @author ningGf
 * @version 1.0.0
 * @since 1.0.0
 **/

@MybatisPlusTest
@ContextConfiguration(classes = TestApp.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(MyBatisPlusAutoConfiguration.class)
@Import(MyBatisPlusConfig.class)
@Sql("classpath:/schema.sql")
class MybatisPlusConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Resource(type = UserMapper.class)
    private UserMapper userMapper;

    @Resource(type = RoleMapper.class)
    private RoleMapper roleMapper;

    @BeforeEach
    void create() {
        {
            var user = new User();
            user.setName("1");
            user.setUid("1");
            user.setRoles(Arrays.asList("1", "2", "3"));
            userMapper.insert(user);

            user = new User();
            user.setName("2");
            user.setUid("2");
            userMapper.insert(user);

            user = new User();
            user.setName("3");
            user.setUid("3");
            userMapper.insert(user);

            user = new User();
            user.setName("4");
            user.setUid("4");
            userMapper.insert(user);

            user = new User();
            user.setName("5");
            user.setUid("5");
            userMapper.insert(user);
        }

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
    void testUser() {
        assertThat(context).isNotNull();
        val user1 = userMapper.selectById("1");
        assertThat(user1).isNotNull();
        val user = userMapper.getUserById("1");
        assertThat(user).isNotNull();
        assertThat(user.getVer()).isEqualTo(0);
        assertThat(user.getRoles()).isNotEmpty().contains("1", "2", "3");

        user.setName("22");
        userMapper.updateById(user);

        user1.setName("2222");
        assertThat(userMapper.updateById(user1)).isEqualTo(0);

        IPage<User> page = new Page<>();
        page.setSize(2);
        userMapper.selectPage(page, null);
        assertThat(page).isNotNull();
        assertThat(page.getTotal()).isEqualTo(5);
        assertThat(page.getPages()).isEqualTo(3);
    }

    @Test
    void testRole() {
        var role = roleMapper.selectById("1");
        assertThat(role).isNotNull();

        role = roleMapper.getRoleById("1");
        assertThat(role).isNotNull();
        assertThat(role.getDd()).isEqualTo(0.25d);
    }

}
