package com.apzda.cloud.demo.bar.facade;

import com.apzda.cloud.demo.bar.proto.BarReq;
import com.apzda.cloud.demo.bar.proto.BarRes;
import com.apzda.cloud.demo.bar.proto.BarService;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;
import java.util.Random;

/**
 * @author fengz
 */
@Service
@Slf4j
public class BarServiceImpl implements BarService {

    @Override
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
        return Flux.fromIterable(List.of(request, request)).map(barReq -> {
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
            log.info("处理请求: {}", barReq);
            return BarRes.newBuilder()
                .setAge(barReq.getAge() + 3)
                .setFileCount(new Random().nextInt())
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

}
