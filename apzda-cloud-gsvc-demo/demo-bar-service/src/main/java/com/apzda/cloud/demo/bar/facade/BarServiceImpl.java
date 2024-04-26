package com.apzda.cloud.demo.bar.facade;

import com.apzda.cloud.demo.bar.proto.BarReq;
import com.apzda.cloud.demo.bar.proto.BarRes;
import com.apzda.cloud.demo.bar.proto.BarService;
import com.apzda.cloud.demo.math.proto.MathService;
import com.apzda.cloud.demo.math.proto.OpNum;
import com.apzda.cloud.demo.math.proto.Request;
import com.apzda.cloud.gsvc.core.GsvcContextHolder;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.google.protobuf.Empty;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponseException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author fengz
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Tag(name = "BarService")
public class BarServiceImpl implements BarService {

    private final MathService mathService;

    private final ObservationRegistry observationRegistry;

    @Value("${management.tracing.enabled:false}")
    private Boolean enabled;

    @Override
    @Operation(summary = "打个招呼", description = "测试的的的的")
    public BarRes greeting(BarReq request) {
        return BarRes.newBuilder().setAge(request.getAge() + 1).setName(request.getName() + ".bar@greeting").build();
    }

    @Override
    public Flux<BarRes> hello(BarReq request) {
        log.error("Tracing Enabled: {}", enabled);

        for (GsvcExt.UploadFile uploadFile : request.getFilesList()) {
            if (uploadFile.getSize() > 0) {
                log.info("删除 File: {}", uploadFile.getFilename());
                new File(uploadFile.getFile()).delete();
            }
            else {
                log.error("文件上传失败: {}", uploadFile.getError());
            }
        }

        val res = BarRes.newBuilder()
            .setAge(request.getAge() + 2)
            .setName(request.getName() + ".bar@hello")
            .setFileCount(request.getFilesCount())
            .build();
        val traceEnabled = enabled;
        val context = GsvcContextHolder.getContext();
        val observation = Observation.createNotStarted("bridge", this.observationRegistry);

        return Flux.create((sink) -> {
            CompletableFuture.runAsync(() -> {
                context.restore();
                sink.next(res);
                if (Boolean.TRUE.equals(traceEnabled)) {
                    observation.lowCardinalityKeyValue("key", "math.hello");
                    observation.observe(() -> {
                        mathService.translate(Request.newBuilder().setKey("math.hello").build());
                        mathService
                            .sum(Flux.just(OpNum.newBuilder().setNum1(1).build(),
                                    OpNum.newBuilder().setNum1(2).build()))
                            .blockLast();
                    });
                }
                sink.complete();
            });
        });
    }

    @Override
    public Flux<BarRes> hi(BarReq request) {
        val atomicInteger = new AtomicInteger();
        val context = GsvcContextHolder.getContext();
        return Flux.fromIterable(List.of(request, request)).publishOn(Schedulers.boundedElastic()).map(barReq -> {
            context.restore();
            try {
                for (GsvcExt.UploadFile uploadFile : barReq.getFilesList()) {
                    if (uploadFile.getSize() > 0) {
                        log.info("删除 File: {}", uploadFile.getFilename());
                        new File(uploadFile.getFile()).delete();
                    }
                    else {
                        log.error("文件上传失败: {}", uploadFile.getError());
                    }
                }
            }
            catch (Exception ignored) {

            }
            val rst = mathService.add(OpNum.newBuilder()
                .setNum1(atomicInteger.incrementAndGet())
                .setNum2(atomicInteger.incrementAndGet())
                .build());
            val result = mathService.square(Flux.just(1, 2).map(n -> OpNum.newBuilder().setNum1(n).build()))
                .publishOn(Schedulers.boundedElastic())
                .blockLast();
            log.info("[{}] 处理请求: {}, 最后一个数的平方: {}", GsvcContextHolder.getRequestId(), barReq, result.getResult());
            return BarRes.newBuilder()
                .setAge(barReq.getAge() + 3)
                .setFileCount((int) rst.getResult())
                .setName(barReq.getName() + ".bar@hi")
                .build();
        }).contextCapture();
    }

    @Override
    public Flux<BarRes> clientStreaming(Flux<BarReq> request) {
        return null;
    }

    @Override
    public Flux<BarRes> bidiStreaming(Flux<BarReq> request) {
        return null;
    }

    @Override
    public GsvcExt.CommonRes err(Empty request) {
        mathService.add(OpNum.newBuilder().setNum1(1).setNum2(2).build());
        throw new ErrorResponseException(HttpStatus.UNAUTHORIZED);
    }

    @Override
    public GsvcExt.CommonRes rpcErr(Empty request) {
        mathService.divide(OpNum.newBuilder().setNum1(1).setNum2(0).build());
        return GsvcExt.CommonRes.newBuilder().setErrCode(0).build();
    }

    @Override
    public GsvcExt.CommonRes ipAddr(Empty request) {
        return mathService.ipAddr(request);
    }

}
