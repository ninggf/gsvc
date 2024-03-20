package com.apzda.cloud.demo.bar.facade;

import com.apzda.cloud.demo.bar.proto.BarReq;
import com.apzda.cloud.demo.bar.proto.BarRes;
import com.apzda.cloud.demo.bar.proto.BarService;
import com.apzda.cloud.demo.math.proto.MathService;
import com.apzda.cloud.demo.math.proto.OpNum;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.google.protobuf.Empty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponseException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.util.List;
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

    @Override
    @Operation(summary = "打个招呼", description = "测试的的的的")
    public BarRes greeting(BarReq request) {
        return BarRes.newBuilder().setAge(request.getAge() + 1).setName(request.getName() + ".bar@greeting").build();
    }

    @Override
    public Flux<BarRes> hello(BarReq request) {

        for (GsvcExt.UploadFile uploadFile : request.getFilesList()) {
            if (uploadFile.getSize() > 0) {
                log.info("删除 File: {}", uploadFile.getFilename());
                new File(uploadFile.getFile()).delete();
            }
            else {
                log.error("文件上传失败: {}", uploadFile.getError());
            }
        }
        var res = BarRes.newBuilder()
            .setAge(request.getAge() + 2)
            .setName(request.getName() + ".bar@hello")
            .setFileCount(request.getFilesCount())
            .build();
        return Flux.just(res);
    }

    @Override
    public Flux<BarRes> hi(BarReq request) {
        val atomicInteger = new AtomicInteger();
        return Flux.fromIterable(List.of(request, request)).publishOn(Schedulers.boundedElastic()).map(barReq -> {
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
            log.info("处理请求: {}, 最后一个数的平方: {}", barReq, result.getResult());
            return BarRes.newBuilder()
                .setAge(barReq.getAge() + 3)
                .setFileCount((int) rst.getResult())
                .setName(barReq.getName() + ".bar@hi")
                .build();
        });
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
        throw new ErrorResponseException(HttpStatus.UNAUTHORIZED);
    }

    @Override
    public GsvcExt.CommonRes rpcErr(Empty request) {
        mathService.divide(OpNum.newBuilder().setNum1(1).setNum2(0).build());
        return GsvcExt.CommonRes.newBuilder().setErrCode(0).build();
    }

}
