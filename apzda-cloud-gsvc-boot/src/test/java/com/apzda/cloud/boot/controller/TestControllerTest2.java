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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * @author fengz (windywany@gmail.com)
 * @version 1.0.0
 * @since 1.0.0
 **/
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestApp.class)
@ImportAutoConfiguration({ GsvcBootAutoConfiguration.class })
@ComponentScan("com.apzda.cloud.boot.service")
@Sql("classpath:/schema.sql")
public class TestControllerTest2 {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void test() throws Exception {

        webTestClient.post()
            .uri("/user")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("pageSize=10&pageNumber=0&pageSorts=uid|desc,createdAt|asc&id=gt 1")
            .exchange()
            .expectStatus()
            .isUnauthorized();
    }

    @Test
    void testExport() throws Exception {
        webTestClient.mutateWith(((builder, httpHandlerBuilder, connector) -> {
            builder.responseTimeout(Duration.ofSeconds(30));
        }))
            .get()
            .uri("/user/export")
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentDisposition(ContentDisposition.attachment().filename("test.xlsx", StandardCharsets.UTF_8).build())
            .expectHeader()
            .contentTypeCompatibleWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

}
