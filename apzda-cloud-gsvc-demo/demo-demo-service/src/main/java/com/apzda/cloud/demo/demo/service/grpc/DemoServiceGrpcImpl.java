package com.apzda.cloud.demo.demo.service.grpc;

import com.apzda.cloud.demo.demo.proto.DemoReq;
import com.apzda.cloud.demo.demo.proto.DemoRes;
import com.apzda.cloud.demo.demo.proto.DemoService;
import com.apzda.cloud.demo.demo.proto.DemoServiceGrpc;
import com.apzda.cloud.gsvc.grpc.GrpcService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

/**
 * @author fengz
 */
@GrpcService
@Slf4j
@RequiredArgsConstructor
public class DemoServiceGrpcImpl extends DemoServiceGrpc.DemoServiceImplBase {

    private final DemoService demoService;

    @Override
    public void greeting(DemoReq request, StreamObserver<DemoRes> responseObserver) {
        try {
            val greeting = demoService.greeting(request);
            responseObserver.onNext(greeting);
            responseObserver.onCompleted();
        }
        catch (Exception e) {
            responseObserver.onError(e);
        }
    }

}
