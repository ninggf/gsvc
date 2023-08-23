package com.example.inventory.grpc;

import com.apzda.cloud.gsvc.proto.HelloReq;
import com.apzda.cloud.gsvc.proto.HelloRes;
import com.apzda.cloud.gsvc.proto.InventoryService;
import com.apzda.cloud.gsvc.proto.InventoryServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * @author ninggf
 */
@GrpcService
@RequiredArgsConstructor
public class InventoryServiceGrpcImpl extends InventoryServiceGrpc.InventoryServiceImplBase {
    private final InventoryService inventoryService;

    @Override
    public void sayHello(HelloReq request, StreamObserver<HelloRes> responseObserver) {
        // unary
        responseObserver.onNext(inventoryService.sayHello(request));
    }

    @Override
    public void sayHei(HelloReq request, StreamObserver<HelloRes> responseObserver) {
        // server-streaming
        val helloResMono = inventoryService.sayHei(request);
        helloResMono
            .doFinally((t) -> responseObserver.onCompleted())
            .onErrorReturn(HelloRes.getDefaultInstance())
            .subscribe((s) -> {
                val builder = HelloRes.newBuilder();
                builder.setName(s.getName() + " - grpc server - sayHei");
                responseObserver.onNext(builder.build());
            });
    }

    @Override
    public StreamObserver<HelloReq> sayYoho(StreamObserver<HelloRes> responseObserver) {
        // client-streaming
        return new StreamObserver<HelloReq>() {
            @Override
            public void onNext(HelloReq value) {
                val name = value.getName() + " + grpc server - sayYoho";
                val res = HelloRes.newBuilder().setName(name);
                responseObserver.onNext(res.build());
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<HelloReq> sayHi(StreamObserver<HelloRes> responseObserver) {
        // bidi-streaming

        return new StreamObserver<HelloReq>() {
            @Override
            public void onNext(HelloReq value) {
                val name = value.getName() + " + grpc server - sayHi";
                val res = HelloRes.newBuilder().setName(name);
                responseObserver.onNext(res.build());
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}
