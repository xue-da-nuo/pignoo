package com.xuesinuo.pignoo.entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.xuesinuo.pignoo.annotation.Column;
import com.xuesinuo.pignoo.annotation.PrimaryKey;
import com.xuesinuo.pignoo.annotation.Table;

public class ClassInfo<E> {
    protected Constructor<E> constructor;
    protected String tableName;

    protected Boolean autoPrimaryKey;
    protected Field primaryKeyField;
    protected String primaryKeyColumn;
    protected Method primaryKeyGetter;
    protected Method primaryKeySetter;

    protected List<Field> fields = new ArrayList<>();
    protected List<String> columns = new ArrayList<>();
    protected List<Method> getters = new ArrayList<>();
    protected List<Method> setters = new ArrayList<>();
    protected List<String> getterNames = new ArrayList<>();
    protected List<String> setterNames = new ArrayList<>();

    public ClassInfo(Class<E> c) {
        Table tableAnn = c.getAnnotation(Table.class);
        this.tableName = tableAnn.value();
        try {
            constructor = c.getDeclaredConstructor();
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
        Field[] classFields = c.getDeclaredFields();
        for (Field field : classFields) {
            if (field.isAnnotationPresent(PrimaryKey.class) && field.isAnnotationPresent(Column.class)) {
                PrimaryKey primaryKeyAnn = field.getAnnotation(PrimaryKey.class);
                Column columnAnn = field.getAnnotation(Column.class);
                this.autoPrimaryKey = primaryKeyAnn.auto();
                this.primaryKeyField = field;
                this.primaryKeyColumn = columnAnn.value();
                Method[] getterSetter = this.fields2GetterSetter(c, field);
                this.primaryKeyGetter = getterSetter[0];
                this.primaryKeySetter = getterSetter[1];
            }
            if (field.isAnnotationPresent(Column.class)) {
                fields.add(field);
                columns.add(field.getAnnotation(Column.class).value());
                Method[] getterSetter = this.fields2GetterSetter(c, field);
                this.getters.add(getterSetter[0]);
                this.setters.add(getterSetter[1]);
                this.getterNames.add(getterSetter[0].getName());
                this.setterNames.add(getterSetter[1].getName());
            }
        }
    }

    private Method[] fields2GetterSetter(Class<E> c, Field field) {
        String fieldName = field.getName();
        String capitalizedFieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        String getterName = "get" + capitalizedFieldName;
        String setterName = "set" + capitalizedFieldName;
        if (field.getType() == boolean.class) {
            if (fieldName.length() > 2 && fieldName.substring(0, 2).equals("is")) {
                if (!Character.isLowerCase(fieldName.charAt(2))) {
                    getterName = fieldName;
                }
            } else {
                getterName = "is" + capitalizedFieldName;
            }
        }
        Method getter;
        try {
            getter = c.getMethod(getterName);
        } catch (Exception e) {
            e.printStackTrace();
            getter = null;
        }
        Method setter;
        try {
            setter = c.getMethod(setterName, field.getType());
        } catch (Exception e) {
            e.printStackTrace();
            setter = null;
        }
        return new Method[] { getter, setter };
    }
}
