package com.apzda.cloud.demo.foo.service;

import com.apzda.cloud.demo.bar.proto.BarReq;
import com.apzda.cloud.demo.bar.proto.BarService;
import com.apzda.cloud.demo.bar.proto.SaReq;
import com.apzda.cloud.demo.bar.proto.SaService;
import com.apzda.cloud.demo.foo.proto.FooReq;
import com.apzda.cloud.demo.foo.proto.FooRes;
import com.apzda.cloud.demo.foo.proto.FooService;
import com.apzda.cloud.gsvc.context.CurrentUserProvider;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.google.protobuf.Empty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author fengz
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FooServiceImpl implements FooService {

    private final BarService barService;

    private final SaService saService;

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
    public Flux<FooRes> hello(FooReq request) {
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
    public Flux<FooRes> hi(FooReq request) {
        val req = BarReq.newBuilder().setName(request.getName() + ".foo3").setAge(request.getAge() + 3).build();

        return barService.hi(req).map(res -> {
            log.warn("收到 barService.hi响应: {}", res);
            return FooRes.newBuilder().setErrCode(0).setName(res.getName()).setAge(res.getAge()).build();
        });
    }

    @Override
    @PreAuthorize("hasPermission(#request.name,'view:/foo/info')")
    public FooRes saInfo(FooReq request) {
        val currentUser = CurrentUserProvider.getCurrentUser();
        val cu = GsvcExt.CurrentUser.newBuilder().setUid(currentUser.getUid()).buildPartial();
        val saReq = SaReq.newBuilder().setName(request.getName()).setCurrentUser(cu).buildPartial();
        val info = saService.info(saReq);

        return FooRes.newBuilder()
            .setErrMsg(info.getErrMsg())
            .setErrCode(info.getErrCode())
            .setName(info.getUserName())
            .buildPartial();
    }

    @Override
    public GsvcExt.CommonRes err(Empty request) {
        try {
            val resp = barService.err(request);
            log.trace("BarService.err: {}", resp);
            return resp;
        }
        catch (Exception e) {
            log.trace("BarService.err, Exception: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public GsvcExt.CommonRes sleep2(Empty request) {
        try {
            TimeUnit.SECONDS.sleep(2);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.trace("Sleep2 complete");
        return GsvcExt.CommonRes.newBuilder().setErrCode(0).build();
    }

    @Override
    public Flux<GsvcExt.CommonRes> sleep3(Empty request) {
        val context = GsvcContextHolder.getContext();
        return Flux.create((sink) -> {
            CompletableFuture.runAsync(() -> {
                context.restore();
                try {
                    TimeUnit.SECONDS.sleep(2);
                    log.trace("Sleep3 complete");
                    sink.next(GsvcExt.CommonRes.newBuilder().setErrCode(0).build());
                    sink.complete();
                }
                catch (InterruptedException e) {
                    sink.error(e);
                }
            });
        });
    }

    @Override
    public GsvcExt.CommonRes ipAddr(Empty request) {
        return barService.ipAddr(request);
    }

}
