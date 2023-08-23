package com.demo.webfux.its;


import com.demo.webfux.model.User;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class DemoControllerItTest {
    private WebClient webClient;

    @BeforeEach
    public void setUp() {
        String port = System.getProperty("test.server.port");
        webClient = WebClient.builder().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void should_get_three_users() {
        val users = webClient.get()
                             .uri("/demo/users")
                             .retrieve()
                             .bodyToFlux(User.class)
                             .timeout(Duration.ofSeconds(30)).toIterable();

        assertThat(users).isNotEmpty();
        assertThat(users).hasSize(3);
    }
}
