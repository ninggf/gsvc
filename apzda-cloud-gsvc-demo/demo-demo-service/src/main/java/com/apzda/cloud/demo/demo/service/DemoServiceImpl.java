package com.apzda.cloud.demo.demo.service;

import com.apzda.cloud.demo.demo.proto.DemoReq;
import com.apzda.cloud.demo.demo.proto.DemoRes;
import com.apzda.cloud.demo.demo.proto.DemoService;
import com.apzda.cloud.demo.foo.proto.FooService;
import com.apzda.cloud.gsvc.ext.GsvcExt;
import com.google.protobuf.Empty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author fengz
 */
@Service
@RequiredArgsConstructor
public class DemoServiceImpl implements DemoService {

    private final FooService fooService;

    @Override
    public DemoRes greeting(DemoReq request) {
        return DemoRes.newBuilder().setName("Hello " + request.getName()).build();
    }

    @Override
    public GsvcExt.CommonRes err(Empty request) {
        return fooService.err(request);
    }

    @Override
    public GsvcExt.CommonRes ipAddr(Empty request) {
        return fooService.ipAddr(request);
    }

    @Override
    public DemoRes enc(DemoReq request) {
        DemoRes.Builder builder = DemoRes.newBuilder();
        return builder.setName("Hello " + request.getName()).build();
    }

    @Override
    public DemoRes apiEnc(DemoReq request) {
        DemoRes.Builder builder = DemoRes.newBuilder();
        return builder.setName("Hello " + request.getName()).build();
    }

}
