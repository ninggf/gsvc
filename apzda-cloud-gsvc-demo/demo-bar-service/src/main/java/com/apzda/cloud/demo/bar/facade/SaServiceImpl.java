package com.apzda.cloud.demo.bar.facade;

import com.apzda.cloud.demo.bar.proto.*;
import org.springframework.stereotype.Service;

/**
 * @author fengz
 */
@Service
public class SaServiceImpl implements SaService {

    @Override
    public LoginRes login(LoginReq request) {
        return LoginRes.newBuilder()
            .setErrCode(0)
            .setAccessToken("access token for " + request.getUsername())
            .setRefreshToken("refresh token for " + request.getPassword())
            .build();
    }

    @Override
    public SaRes info(SaReq request) {
        return SaRes.newBuilder().setUserName(request.getName() + ".sa").setErrCode(0).buildPartial();
    }

}
