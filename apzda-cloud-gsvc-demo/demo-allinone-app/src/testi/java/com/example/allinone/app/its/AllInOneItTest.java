package com.example.allinone.app.its;

import com.apzda.cloud.gsvc.utils.ResponseUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static com.apzda.cloud.gsvc.utils.ResponseUtils.OBJECT_MAPPER;

public class AllInOneItTest {

    private WebClient webClient;

    static {
        ResponseUtils.config();
    }

    @BeforeEach
    public void setUp() {
        String port = System.getProperty("test.server.port");
        webClient = WebClient.builder().baseUrl("http://localhost:" + port).build();
    }

    private <T> Mono<T> handleResponseBody(ClientResponse response, Class<T> tClass) {
        if (response.statusCode().isError()) {
            return Mono.error(new IllegalStateException(response.statusCode().toString()));
        }
        return response.bodyToMono(String.class).handle((str, sink) -> {
            try {
                sink.next(OBJECT_MAPPER.readValue(str, tClass));
                sink.complete();
            }
            catch (JsonProcessingException e) {
                sink.error(e);
            }
        });
    }

}
