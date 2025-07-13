package com.xuesinuo.pignoo.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个属性，作为数据库主键
 * <p>
 * Mark a entity field , as a primary key in database
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface PrimaryKey {
    /**
     * @return 是否自动生成
     *         <p>
     *         Whether to automatically generate
     */
    boolean auto();
}