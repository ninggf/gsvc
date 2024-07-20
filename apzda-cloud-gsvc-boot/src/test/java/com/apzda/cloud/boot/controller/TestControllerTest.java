/*
 * Copyright (C) 2023-2024 Fengz Ning (windywany@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apzda.cloud.boot.controller;

import com.apzda.cloud.boot.TestApp;
import com.apzda.cloud.boot.autoconfig.GsvcBootAutoConfiguration;
import com.apzda.cloud.gsvc.security.config.GsvcSecurityAutoConfiguration;
import com.apzda.cloud.test.autoconfig.AutoConfigureGsvcTest;
import com.baomidou.mybatisplus.test.autoconfigure.AutoConfigureMybatisPlus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@WebMvcTest(TestController.class)
@ContextConfiguration(classes = TestApp.class)
@ImportAutoConfiguration(classes = { GsvcBootAutoConfiguration.class, GsvcSecurityAutoConfiguration.class })
@ComponentScan(basePackageClasses = TestApp.class)
@AutoConfigureMockMvc
@AutoConfigureMybatisPlus
@AutoConfigureGsvcTest
@Sql("classpath:/schema.sql")
public class TestControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(authorities = "r:user.*")
    void test() throws Exception {
        val content = mvc
            .perform(post("/user").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("pageSize=10&pageNumber=0&pageSorts=uid|desc,createdAt|asc&id=gt 1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.records[0].id").value("2"));
    }

}
