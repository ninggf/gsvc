package com.example.inventory.service;


import cn.dev33.satoken.stp.StpUtil;
import com.apzda.cloud.gsvc.proto.HelloReq;
import com.apzda.cloud.gsvc.proto.HelloRes;
import com.apzda.cloud.gsvc.proto.InventoryService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

/**
 * @author fengz
 */
@Service
@Slf4j
public class InventoryServiceImpl implements InventoryService {
    @Override
    public HelloRes sayHello(HelloReq request) {
        // unary
        val helloResp = HelloRes.newBuilder();
        val s = StpUtil.getSession().get("aaa", "aaa");
        helloResp.setName("你好, " + request.getName() + " session data:  " + s + ", uid:" + request.getCurrentUser()
            .getUid());
        return helloResp.build();
    }

    @Override
    public Mono<HelloRes> sayHei(HelloReq request) {
        // server-streaming
        val helloResp = HelloRes.newBuilder();

        helloResp.setName("[server-streaming]你好, " + request.getName());
        return Mono.just(helloResp.build());
    }

    @Override
    public Mono<HelloRes> sayYoho(Mono<HelloReq> request) {
        return request.map((res) -> {
            val builder = HelloRes.newBuilder();
            builder.setName(res.getName() + " by sayYoho [bidi-streaming]");
            return builder.build();
        });
    }

    @Override
    public Mono<HelloRes> sayHi(Mono<HelloReq> request) {
        request.handle((res, sink) -> {
            val builder = HelloRes.newBuilder();
            builder.setName(res.getName() + " by sayHi [bidi-streaming]");
            try {
                TimeUnit.SECONDS.sleep(10);
                sink.next(builder.build());
                sink.complete();
            } catch (InterruptedException e) {
                sink.error(e);
            }
        });

        return Mono.error(new NullPointerException());
    }
}
