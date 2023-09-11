package com.apzda.cloud.demo.foo.service;

import com.apzda.cloud.demo.bar.proto.BarReq;
import com.apzda.cloud.demo.bar.proto.BarService;
import com.apzda.cloud.demo.bar.proto.SaReq;
import com.apzda.cloud.demo.bar.proto.SaService;
import com.apzda.cloud.demo.foo.proto.FooReq;
import com.apzda.cloud.demo.foo.proto.FooRes;
import com.apzda.cloud.demo.foo.proto.FooService;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * @author fengz
 */
@Service
public class FooServiceImpl implements FooService {

    @Autowired
    private BarService barService;

    @Autowired
    private SaService saService;

    @Override
    public FooRes greeting(FooReq request) {
        val req = BarReq.newBuilder().setName(request.getName() + ".foo").setAge(request.getAge() + 1).build();
        val res = barService.greeting(req);
        if (res.getErrCode() == 0) {
            return FooRes.newBuilder().setErrCode(0).setName(res.getName()).setAge(res.getAge()).build();
        }
        return FooRes.newBuilder().setErrCode(res.getErrCode()).setErrMsg(res.getErrMsg()).build();
    }

    @Override
    public Mono<FooRes> hello(FooReq request) {
        val req = BarReq.newBuilder().setName(request.getName() + ".foo2").setAge(request.getAge() + 2).build();
        return barService.hello(req)
            .map(res -> FooRes.newBuilder()
                .setErrCode(res.getErrCode())
                .setErrMsg(res.getErrMsg())
                .setName(res.getName())
                .setAge(res.getAge())
                .build());
    }

    @Override
    public Mono<FooRes> hi(Mono<FooReq> request) {
        return request
            .map(fooReq -> BarReq.newBuilder().setName(fooReq.getName() + ".foo3").setAge(fooReq.getAge() + 3).build())
            .transform(barService::hi)
            .map(res -> FooRes.newBuilder().setErrCode(0).setName(res.getName()).setAge(res.getAge()).build());
    }

    @Override
    public FooRes saInfo(FooReq request) {
        val saReq = SaReq.newBuilder().setName(request.getName()).buildPartial();
        val info = saService.info(saReq);

        return FooRes.newBuilder()
            .setErrMsg(info.getErrMsg())
            .setErrCode(info.getErrCode())
            .setName(info.getUserName())
            .buildPartial();
    }

}
