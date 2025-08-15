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
 * @version 1.0.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface Column {
    /**
     * 强制忽略此字段，不进行映射
     * <p>
     * Force ignore this field, not mapping
     * 
     * @return 是否忽略
     *         <p>
     *         Whether to ignore
     */
    boolean ignore() default false;

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
     * 主键设置
     * <p>
     * Primary key setting
     * 
     * @return 主键设置枚举
     *         <p>
     *         Primary key setting enum
     * @since 1.0.0
     */
    PrimaryKey primaryKey() default PrimaryKey.DEFAULT;

    /**
     * Pignoo中预设的规模档位
     * <p>
     * Preset scale level in Pignoo
     * 
     * @since 0.3.2
     */
    public static final class PresetScale {
        /**
         * 小规模数据
         * <p>
         * Small scale data
         * 
         * @since 0.3.1
         */
        public static final int SMALL = 1;
        /**
         * 中规模数据
         * <p>
         * Medium scale data
         * 
         * @since 0.3.1
         */
        public static final int MEDIUM = 2;
        /**
         * 大规模数据
         * <p>
         * Large scale data
         * 
         * @since 0.3.1
         */
        public static final int LARGE = 3;
    }

    /**
     * 主键设置枚举
     * <p>
     * Primary key setting enum
     * 
     * @since 1.0.0
     */
    public static enum PrimaryKey {
        /**
         * 是自动生成的主键
         * <p>
         * Is an auto-generated primary key
         */
        AUTO,
        /**
         * 不是自动生成的主键
         * <p>
         * Is not an auto-generated primary key
         */
        NON_AUTO,
        /**
         * 不是主键
         * <p>
         * Is not a primary key
         */
        NOT,
        /**
         * 默认值：没声明是否为主键
         * <p>
         * Default value: not declared whether it is a primary key
         */
        DEFAULT;
    }
}
