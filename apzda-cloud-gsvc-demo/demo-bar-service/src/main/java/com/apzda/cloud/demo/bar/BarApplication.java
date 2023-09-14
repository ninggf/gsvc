package com.apzda.cloud.demo.bar;

import com.apzda.cloud.demo.bar.proto.BarServiceGrpc;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoMethodDescriptorSupplier;
import io.grpc.protobuf.ProtoServiceDescriptorSupplier;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

/**
 * @author fengz
 */
@SpringBootApplication
@PropertySource("classpath:bar.service.properties")
@Slf4j
public class BarApplication {

    public static void main(String[] args) {
        val descriptor = BarServiceGrpc.getServiceDescriptor();
        log.info("getSchemaDescriptor: {}", descriptor.getSchemaDescriptor());
        val methods = descriptor.getMethods();
        for (MethodDescriptor<?, ?> method : methods) {
            log.info("method -> name: {}, options: {}", method.getBareMethodName(),
                    ((ProtoMethodDescriptorSupplier) method.getSchemaDescriptor()).getMethodDescriptor().getOptions());
        }
        if (descriptor.getSchemaDescriptor() instanceof ProtoServiceDescriptorSupplier supplier) {
            log.info("options: {}", supplier.getServiceDescriptor().getOptions());
        }

        SpringApplication.run(BarApplication.class, args);
    }

}
