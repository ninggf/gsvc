package com.apzda.cloud.test.autoconfig.json;

import com.apzda.cloud.test.autoconfig.GsvcTest;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.boot.test.autoconfigure.filter.StandardAnnotationCustomizableTypeExcludeFilter;
import org.springframework.util.ClassUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class JsonTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<GsvcTest> {

    private static final String JACKSON_MODULE = "com.fasterxml.jackson.databind.Module";

    private static final Set<Class<?>> DEFAULT_INCLUDES;

    static {
        Set<Class<?>> includes = new LinkedHashSet<>();
        try {
            includes.add(ClassUtils.forName(JACKSON_MODULE, null));
        }
        catch (Exception ex) {
            // Ignore
        }
        includes.add(JsonComponent.class);
        DEFAULT_INCLUDES = Collections.unmodifiableSet(includes);
    }

    JsonTypeExcludeFilter(Class<?> testClass) {
        super(testClass);
    }

    @Override
    protected Set<Class<?>> getDefaultIncludes() {
        return DEFAULT_INCLUDES;
    }

}
