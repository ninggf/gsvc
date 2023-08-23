package com.example.allinone.app.its;

import com.apzda.cloud.gsvc.ResponseUtils;
import com.example.order.proto.LoginRes;
import com.example.order.proto.OrderHelloRequest;
import com.example.order.proto.OrderHelloResp;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.ByteString;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;

import static com.apzda.cloud.gsvc.ResponseUtils.OBJECT_MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void should_login_success() throws JsonProcessingException {
        val args = new HashMap<String, String>() {{
            put("username", "12");
            put("password", "34");
        }};
        val loginRes = webClient.post()
                                .uri("/demo/orderService/login")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(args)
                                .exchangeToMono(clientResponse ->
                                                    handleResponseBody(clientResponse, LoginRes.class))
                                .block();

        assertThat(loginRes).isNotNull();
        assertThat(loginRes.getToken()).isNotBlank();
        val tokenName = loginRes.getTokenName();
        val tokenValue = loginRes.getTokenValue();


        val file = ByteString.copyFromUtf8("你好呀");
        val req = OrderHelloRequest.newBuilder().setAAge(18).setName("gsvc").setFile(file).build();

        val helloRes = webClient.post()
                                .uri("/demo/orderService/sayHello")
                                .accept(MediaType.APPLICATION_JSON)
                                .header(tokenName, tokenValue)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(OBJECT_MAPPER.writeValueAsBytes(req))
                                .exchangeToMono(clientResponse -> handleResponseBody(clientResponse,
                                                                                     OrderHelloResp.class)).block();
        assertThat(helloRes).isNotNull();
        assertThat(helloRes.getErrCode()).isEqualTo(0);
        assertThat(helloRes.getName()).isEqualTo("你好, gsvc session data:  666, uid:12345");
    }

    private <T> Mono<T> handleResponseBody(ClientResponse response, Class<T> tClass) {
        if (response.statusCode().isError()) {
            return Mono.error(new IllegalStateException(response.statusCode().toString()));
        }
        return response.bodyToMono(String.class).handle((str, sink) -> {
            try {
                sink.next(OBJECT_MAPPER.readValue(str, tClass));
                sink.complete();
            } catch (JsonProcessingException e) {
                sink.error(e);
            }
        });
    }
}
