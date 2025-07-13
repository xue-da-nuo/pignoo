package com.xuesinuo.pignoo.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记属性，映射到数据库列
 * <p>
 * Mark a entity field , mapping to a column in database
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface Column {
    /**
     * @return 数据库列名
     *         <p>
     *         Database column name
     */
    String value();
}
