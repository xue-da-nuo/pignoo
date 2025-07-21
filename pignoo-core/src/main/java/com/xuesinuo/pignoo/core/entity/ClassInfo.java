package com.xuesinuo.pignoo.core.entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.annotation.Column;
import com.xuesinuo.pignoo.core.annotation.PrimaryKey;
import com.xuesinuo.pignoo.core.annotation.Table;
import com.xuesinuo.pignoo.core.config.AnnotationMode;
import com.xuesinuo.pignoo.core.config.PrimaryKeyNamingConvention;
import com.xuesinuo.pignoo.core.config.AnnotationMode.AnnotationMixMode;

/**
 * 解析实体类
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
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

    public ClassInfo(Class<E> c, PignooConfig config) {
        config = config.copy();
        if (config.getAnnotationMode() == null) {
            config.setAnnotationMode(AnnotationMode.MIX);
        }
        if (config.getAnnotationMixMode() == null) {
            config.setAnnotationMixMode(AnnotationMixMode.CAMEL_TO_SNAKE);
        }
        Table tableAnn = c.getAnnotation(Table.class);
        if (config.getAnnotationMode() == AnnotationMode.MUST && tableAnn == null) {
            throw new RuntimeException("Entity " + c.getName() + " missing @Table");
        }
        if (tableAnn != null && (tableAnn.value() == null || tableAnn.value().isBlank())) {
            throw new RuntimeException("Entity " + c.getName() + " @Table value can not be empty");
        }
        if (tableAnn != null) {
            this.tableName = tableAnn.value();
        } else if (config.getAnnotationMode() == AnnotationMode.MIX) {
            if (config.getAnnotationMixMode() == AnnotationMixMode.SAME) {
                this.tableName = c.getSimpleName();
            } else if (config.getAnnotationMixMode() == AnnotationMixMode.CAMEL_TO_SNAKE) {
                this.tableName = camel2Underline(c.getSimpleName());
            }
        }
        if (this.tableName == null || this.tableName.isBlank()) {
            throw new RuntimeException("Entity " + c.getName() + " read table name failed");
        }
        try {
            constructor = c.getDeclaredConstructor();
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
        Field[] classFields = c.getDeclaredFields();
        for (Field field : classFields) {
            if (field.isAnnotationPresent(PrimaryKey.class)) {
                PrimaryKey primaryKeyAnn = field.getAnnotation(PrimaryKey.class);
                Column columnAnn = field.getAnnotation(Column.class);
                if (columnAnn == null && config.getAnnotationMode() == AnnotationMode.MUST) {
                    throw new RuntimeException("Entity " + c.getName() + " the primaryKey missing @Column");
                }
                if (columnAnn != null && (columnAnn.value() == null || columnAnn.value().isBlank())) {
                    throw new RuntimeException("Entity " + c.getName() + " the primaryKey @Column value can not be empty");
                }
                if (columnAnn != null) {
                    this.primaryKeyColumn = columnAnn.value();
                } else if (config.getAnnotationMode() == AnnotationMode.MIX) {
                    if (config.getAnnotationMixMode() == AnnotationMixMode.SAME) {
                        this.primaryKeyColumn = field.getName();
                    } else if (config.getAnnotationMixMode() == AnnotationMixMode.CAMEL_TO_SNAKE) {
                        this.primaryKeyColumn = camel2Underline(field.getName());
                    }
                }
                if (this.primaryKeyColumn == null || this.primaryKeyColumn.isBlank()) {
                    throw new RuntimeException("Entity " + c.getName() + " read primaryKey column name failed");
                }
                this.autoPrimaryKey = primaryKeyAnn.auto();
                if (this.primaryKeyField != null) {
                    throw new RuntimeException("Entity " + c.getName() + " can't has more than one @PrimaryKey");
                } else {
                    this.primaryKeyField = field;
                }
                Method[] getterSetter = this.fields2GetterSetter(c, field);
                this.primaryKeyGetter = getterSetter[0];
                this.primaryKeySetter = getterSetter[1];
            }
            if (field.isAnnotationPresent(Column.class) || config.getAnnotationMode() == AnnotationMode.MIX) {
                this.fields.add(field);
                Column columnAnn = field.getAnnotation(Column.class);
                String columnName = null;
                if (columnAnn == null && config.getAnnotationMode() == AnnotationMode.MUST) {
                    throw new RuntimeException("Entity " + c.getName() + "#" + field.getName() + " missing @Column");
                }
                if (columnAnn != null && (columnAnn.value() == null || columnAnn.value().isBlank())) {
                    throw new RuntimeException("Entity " + c.getName() + "#" + field.getName() + " @Column value can not be empty");
                }
                if (columnAnn != null) {
                    columnName = columnAnn.value();
                } else if (config.getAnnotationMode() == AnnotationMode.MIX) {
                    if (config.getAnnotationMixMode() == AnnotationMixMode.SAME) {
                        columnName = field.getName();
                    } else if (config.getAnnotationMixMode() == AnnotationMixMode.CAMEL_TO_SNAKE) {
                        columnName = camel2Underline(field.getName());
                    }
                }
                if (columnName == null || columnName.isBlank()) {
                    throw new RuntimeException("Entity " + c.getName() + "#" + field.getName() + " read column name failed");
                }
                this.columns.add(columnName);
                Method[] getterSetter = this.fields2GetterSetter(c, field);
                this.getters.add(getterSetter[0]);
                this.setters.add(getterSetter[1]);
                this.getterNames.add(getterSetter[0].getName());
                this.setterNames.add(getterSetter[1].getName());
            }
        }
        if (this.primaryKeyField == null) {
            String pkName;
            if (config.getPrimaryKeyNamingConvention() != null) {
                pkName = config.getPrimaryKeyNamingConvention().naming(tableName, c.getSimpleName());
            } else {
                pkName = PrimaryKeyNamingConvention.DEFAULT.naming(tableName, c.getSimpleName());
            }
            if (pkName == null || pkName.isBlank()) {
                throw new RuntimeException("Entity " + c.getName() + " PrimaryKey not found");
            }
            int indexOfPk = this.columns.indexOf(pkName);
            if (indexOfPk < 0) {
                throw new RuntimeException("Entity " + c.getName() + " PrimaryKey not found");
            }
            this.primaryKeyColumn = pkName;
            if (config.getAutoPrimaryKey() == null) {
                this.autoPrimaryKey = true;
            } else {
                this.autoPrimaryKey = config.getAutoPrimaryKey();
            }
            this.primaryKeyField = this.fields.get(indexOfPk);
            this.primaryKeyGetter = this.getters.get(indexOfPk);
            this.primaryKeySetter = this.setters.get(indexOfPk);
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
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException("Entity " + c.getName() + " read getter failed", e);
        }
        Method setter;
        try {
            setter = c.getMethod(setterName, field.getType());
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException("Entity " + c.getName() + " read setter failed", e);
        }
        return new Method[] { getter, setter };
    }

    private String camel2Underline(String str) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (char c : str.toCharArray()) {
            if (i++ == 0) {
                sb.append(Character.toLowerCase(c));
            } else {
                if (Character.isUpperCase(c)) {
                    sb.append("_").append(Character.toLowerCase(c));
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}
