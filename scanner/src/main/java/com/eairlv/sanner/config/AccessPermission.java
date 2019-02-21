package com.eairlv.sanner.config;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 采用显示声明，方式由于遗忘注解而导致内部接口暴露
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface AccessPermission {

    /**
     * jwt校验，如果设置为否则可直接访问
     * @return
     */
    boolean jwt() default true;
}
