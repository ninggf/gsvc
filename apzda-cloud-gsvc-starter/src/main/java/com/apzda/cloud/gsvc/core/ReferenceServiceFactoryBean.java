package com.apzda.cloud.gsvc.core;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.*;
import org.springframework.core.io.ResourceLoader;

import java.lang.reflect.Proxy;

/**
 * @author ninggf
 */
@Slf4j
@RequiredArgsConstructor
@Data
public class ReferenceServiceFactoryBean
        implements FactoryBean<Object>, ApplicationContextAware, MessageSourceAware, ResourceLoaderAware {

    private final String serviceName;

    private final String appName;

    private final String contextPath;

    private final String interfaceName;

    private final int serviceIndex;

    private ApplicationContext applicationContext;

    private MessageSource messageSource;

    private ResourceLoader resourceLoader;

    @Override
    public Object getObject() throws Exception {
        val proxy = new ReferenceServiceProxy(this);
        //todo: 优化，使用GsvcStub替代Proxy.
        return Proxy.newProxyInstance(ReferenceServiceProxy.class.getClassLoader(), new Class[] { getObjectType() },
                proxy);
    }

    @Override
    public Class<?> getObjectType() {
        try {
            return Class.forName(interfaceName);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
