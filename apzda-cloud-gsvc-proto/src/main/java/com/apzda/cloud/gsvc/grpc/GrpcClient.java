package com.apzda.cloud.gsvc.grpc;

import java.lang.annotation.*;

/**
 * @author fengz
 */
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface GrpcClient {

    String value();

}
