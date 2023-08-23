/*
 * This file is part of gsvc created at 2023/8/19 by ningGf.
 */
package com.example.inventory.service;

import com.apzda.cloud.gsvc.proto.GreetingService;
import com.apzda.cloud.gsvc.proto.HelloRequest;
import com.apzda.cloud.gsvc.proto.HelloResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Created at 2023/8/19 10:34.
 *
 * @author ningGf
 * @version 1.0.0
 * @since 1.0.0
 **/
@Service
public class GreetingServiceImpl implements GreetingService {
    // unary
    @Override
    public HelloResponse sayHello(HelloRequest request) {
        int fileCount = 0;
        fileCount += request.getDsFilesCount();
        if (request.hasUploadFile()) {
            fileCount += 1;
        }
        return HelloResponse.newBuilder()
            .setFileCount(fileCount)
            .setCurrentUser(request.getCurrentUser())
            .setName(request.getName() + ", 你好!")
            .build();
    }

    @Override
    public Mono<HelloResponse> sayHei(HelloRequest request) {
        return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Override
    public Mono<HelloResponse> sayHi(Mono<HelloRequest> request) {
        return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
