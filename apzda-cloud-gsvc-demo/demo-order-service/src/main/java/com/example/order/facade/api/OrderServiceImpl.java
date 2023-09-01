package com.example.order.facade.api;

import cn.dev33.satoken.stp.StpUtil;
import com.apzda.cloud.gsvc.proto.HelloReq;
import com.apzda.cloud.gsvc.proto.InventoryService;
import com.example.order.proto.*;
import jakarta.annotation.Resource;
import lombok.val;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author fengz
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Resource(type = InventoryService.class)
    private InventoryService inventoryService;

    @Override
    public Mono<LoginRes> login(LoginReq request) {

        return Mono.create((sink) -> {
            StpUtil.login("12345");
            StpUtil.getSession(true).set("aaa", "666");
            val tokenInfo = StpUtil.getTokenInfo();
            val builder = LoginRes.newBuilder()
                .setToken(tokenInfo.tokenName + "=" + tokenInfo.tokenValue)
                .setTokenValue(tokenInfo.tokenValue)
                .setTokenName(tokenInfo.tokenName);
            sink.success(builder.build());
        });
    }

    @Override
    public OrderHelloResp hi(OrderHelloRequest request) {
        val builder = OrderHelloResp.newBuilder();
        val helloReq = HelloReq.newBuilder().setName(request.getName()).build();
        val helloResMono = inventoryService.sayHi(Mono.just(helloReq));
        val helloRes = helloResMono.subscribeOn(Schedulers.boundedElastic()).block();
        assert helloRes != null;

        builder.setName(helloRes.getName() + " from inventory")
            .setErrCode(helloRes.getErrCode())
            .setErrMsg(helloRes.getErrMsg());

        return builder.build();
    }

    @Override
    public OrderHelloResp sayHello(OrderHelloRequest request) {
        val helloRequest = HelloReq.newBuilder();
        helloRequest.setName(request.getName());
        val helloResp = inventoryService.sayHello(helloRequest.build());
        val resp = OrderHelloResp.newBuilder(OrderHelloResp.getDefaultInstance());
        resp.setName(helloResp.getName())
            .setErrCode(helloResp.getErrCode())
            .setErrMsg(helloResp.getErrMsg())
            .setAge(request.getAAge());
        return resp.build();
    }

    @Override
    public HomeRes home(HomeReq request) {
        val resp = HomeRes.newBuilder(HomeRes.getDefaultInstance());
        return resp.build();
    }

}
