package com.tzg.tools.generator.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.tzg.tools.generator.enums.IdType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TabId {
    /*
     * <p>
     * 字段值（驼峰命名方式，该值可无）
     * </p>
     */
    String value() default "";

    /*
     * <p>
     * 主键ID
     * </p>
     * {@link IdType}
     */
    IdType type() default IdType.NONE;

}
