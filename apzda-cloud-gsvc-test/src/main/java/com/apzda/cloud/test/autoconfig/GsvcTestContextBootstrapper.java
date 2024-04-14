package com.apzda.cloud.test.autoconfig;

import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.TestContextAnnotationUtils;

class GsvcTestContextBootstrapper extends SpringBootTestContextBootstrapper {

    @Override
    protected String[] getProperties(Class<?> testClass) {
        GsvcTest jsonTest = TestContextAnnotationUtils.findMergedAnnotation(testClass, GsvcTest.class);
        return (jsonTest != null) ? jsonTest.properties() : null;
    }

}
