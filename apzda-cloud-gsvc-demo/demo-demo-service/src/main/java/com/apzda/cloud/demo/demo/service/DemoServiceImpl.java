package com.apzda.cloud.demo.demo.service;

import com.apzda.cloud.demo.demo.proto.DemoReq;
import com.apzda.cloud.demo.demo.proto.DemoRes;
import com.apzda.cloud.demo.demo.proto.DemoService;
import org.springframework.stereotype.Service;

/**
 * @author fengz
 */
@Service
public class DemoServiceImpl implements DemoService {

    @Override
    public DemoRes greeting(DemoReq request) {
        return DemoRes.newBuilder().setName("Hello " + request.getName()).build();
    }

}
