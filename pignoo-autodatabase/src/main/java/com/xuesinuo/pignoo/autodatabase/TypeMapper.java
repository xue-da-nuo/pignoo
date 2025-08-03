package com.xuesinuo.pignoo.autodatabase;

@FunctionalInterface
public interface TypeMapper {
    public String javaTypeToSqlType(Class<?> javaType);
}
