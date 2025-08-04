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
 * @version 0.3.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface Column {
    /**
     * 数据库列名
     * <p>
     * Database column name
     * 
     * @return 数据库列名
     *         <p>
     *         Database column name
     */
    String value() default "";

    /**
     * 这个等级，不是真实的数字量级或字符串长度
     * <p>
     * This level is not the real number scale or string length
     * <p>
     * 默认情况下是1～3级的映射关系
     * <p>
     * By default, it is a mapping relationship of 1～3 levels
     * 
     * @return 列的规模等级
     *         <p>
     *         Scale (Level) of the column
     * @since 0.3.1
     */
    int scale() default 0;

    /**
     * 小规模数据
     * <p>
     * Small scale data
     * 
     * @since 0.3.1
     */
    public static final int SCALE_SMALL = 1;

    /**
     * 中规模数据
     * <p>
     * Medium scale data
     * 
     * @since 0.3.1
     */
    public static final int SCALE_MEDIUM = 2;

    /**
     * 大规模数据
     * <p>
     * Large scale data
     * 
     * @since 0.3.1
     */
    public static final int SCALE_LARGE = 3;
}
