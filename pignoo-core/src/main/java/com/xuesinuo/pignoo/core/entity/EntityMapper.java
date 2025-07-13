package com.xuesinuo.pignoo.core.entity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 实体映射器，启动包很类信息、getter方法与属性名的映射器
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
public class EntityMapper<E> {

    private ClassInfo<E> classInfo;
    private FunctionNameGetter<E> functionNameGetter;
    private static final ConcurrentHashMap<Class<?>, EntityMapper<?>> cache = new ConcurrentHashMap<>();

    private EntityMapper(Class<E> c) {
        this.classInfo = new ClassInfo<>(c);
        this.functionNameGetter = new FunctionNameGetter<>(c);
    }

    @SuppressWarnings("unchecked")
    public static <E> EntityMapper<E> build(Class<E> c) {
        EntityMapper<E> mapper = (EntityMapper<E>) cache.get(c);
        if (mapper == null) {
            mapper = new EntityMapper<>(c);
            cache.put(c, mapper);
        }
        return mapper;
    }

    public String tableName() {
        return classInfo.tableName;
    }

    public E buildEntity() {
        try {
            return classInfo.constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> columns() {
        return classInfo.columns;
    }

    public List<Field> fields() {
        return classInfo.fields;
    }

    public List<Method> getters() {
        return classInfo.getters;
    }

    public List<Method> setters() {
        return classInfo.setters;
    }

    public List<String> setterNames() {
        return classInfo.setterNames;
    }

    public String primaryKeyColumn() {
        return classInfo.primaryKeyColumn;
    }

    public Method primaryKeyGetter() {
        return classInfo.primaryKeyGetter;
    }

    public boolean autoPrimaryKey() {
        return classInfo.autoPrimaryKey;
    }

    public String getColumnByFunction(Function<E, ?> function) {
        String functionName = functionNameGetter.getFunctionName(function);
        int index = classInfo.getterNames.indexOf(functionName);
        if (index >= 0) {
            return classInfo.columns.get(index);
        }
        return null;
    }
}
