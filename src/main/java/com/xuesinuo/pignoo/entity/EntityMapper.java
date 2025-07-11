package com.xuesinuo.pignoo.entity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;

/** 需要逐个Entity.class缓存下来的映射 */
public class EntityMapper<E> {

    private ClassInfo<E> classInfo;
    private FunctionNameGetter<E> functionNameGetter;

    public EntityMapper(Class<E> c) {
        this.classInfo = new ClassInfo<>(c);
        this.functionNameGetter = new FunctionNameGetter<>(c);
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

    public String getColumnByFunction(Function<E, ?> function) {
        String functionName = functionNameGetter.getFunctionName(function);
        int index = classInfo.getterNames.indexOf(functionName);
        if (index >= 0) {
            return classInfo.columns.get(index);
        }
        return null;
    }
}
