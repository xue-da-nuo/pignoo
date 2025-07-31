package com.xuesinuo.pignoo.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 映射到一个主实体
 * <p>
 * mapping to another entity
 * 
 * @author xuesinuo
 * @since 0.2.4
 * @version 0.2.4
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Link {
    Class<?> value();
}
