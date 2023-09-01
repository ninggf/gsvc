package com.apzda.cloud.demo.bar.facade;

import com.apzda.cloud.demo.bar.proto.BarReq;
import com.apzda.cloud.demo.bar.proto.BarRes;
import com.apzda.cloud.demo.bar.proto.BarService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class BarServiceImpl implements BarService {

    @Override
    public BarRes greeting(BarReq request) {
        return BarRes.newBuilder().setAge(request.getAge() + 1).setName(request.getName() + ".bar@greeting").build();
    }

    @Override
    public Mono<BarRes> hello(BarReq request) {
        var res = BarRes.newBuilder().setAge(request.getAge() + 2).setName(request.getName() + ".bar@hello").build();
        return Mono.just(res);
    }

    @Override
    public Mono<BarRes> hi(Mono<BarReq> request) {
        return request.map(barReq -> BarRes.newBuilder()
            .setAge(barReq.getAge() + 3)
            .setName(barReq.getName() + ".bar@hi")
            .build());
    }

}
