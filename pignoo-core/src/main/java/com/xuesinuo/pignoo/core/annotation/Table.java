package com.xuesinuo.pignoo.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个实体类，映射到数据库表
 * <p>
 * Mark a entity class, mapping to a table in database
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Table {

    /**
     * @return 数据库表名
     *         <p>
     *         Database table name
     */
    String value();
}
