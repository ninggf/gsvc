package com.apzda.cloud.gsvc.jpa;

import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

@IdGeneratorType(SnowflakeIdGenerator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ FIELD })
public @interface SnowflakeId {

    String prefix() default "";

}
