package com.xuesinuo.pignoo.autodatabase;

/**
 * 数据类型与数据库的映射关系，支持自定义，不做自定义则会使用Pignoo默认的规则
 * <p>
 * Data type mapping relationship with the database, support customization, no customization will use the default rules of Pignoo
 * 
 * @author xuesinuo
 * @since 0.3.0
 * @version 0.3.1
 */
@FunctionalInterface
public interface TypeMapper {
    /**
     * Java类型与数据库类型的映射关系
     * 
     * @param javaType Java类型
     *                 <p>
     *                 Java Type
     * @param scale    数据规模（是等级1～5，支持自定义，不是数据长度）
     *                 <p>
     *                 Data Scale (Level 1-5, support customization, not data length)
     * @return 数据库类型
     *         <p>
     *         Database Type
     */
    public String javaTypeToSqlType(Class<?> javaType, int scale);
}
