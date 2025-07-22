
package com.xuesinuo.pignoo.core.implement;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooFilter;
import com.xuesinuo.pignoo.core.PignooWriter;
import com.xuesinuo.pignoo.core.PignooSorter;
import com.xuesinuo.pignoo.core.entity.EntityProxyFactory;
import com.xuesinuo.pignoo.core.entity.SqlParam;

/**
 * 基于MySQL语法实现的{@link PignooWriter}
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
public class MySqlPignooWriter<E> extends MySqlPignooReadOnlyList<E> implements PignooWriter<E> {

    private final EntityProxyFactory<E> entityProxyFactory;

    public MySqlPignooWriter(Pignoo pignoo, Supplier<Connection> connGetter, Consumer<Connection> connCloser, boolean inTransaction, Class<E> c, PignooConfig config) {
        super(pignoo, connGetter, connCloser, inTransaction, c, config);

        this.entityProxyFactory = new EntityProxyFactory<>(c, entityMapper, (index, arg, e) -> {
            if (pignoo.closed()) {
                return;
            }
            Object primaryKeyValue = null;
            try {
                primaryKeyValue = entityMapper.primaryKeyGetter().invoke(e);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException("Primary key is not found " + e, ex);
            }
            if (primaryKeyValue == null) {
                throw new RuntimeException("Primary key can not be NULL " + e);
            }
            SqlParam sqlParam = new SqlParam();
            StringBuilder sql = new StringBuilder("");
            sql.append("UPDATE ");
            sql.append("`" + entityMapper.tableName() + "` ");
            sql.append("SET ");
            sql.append("`" + entityMapper.columns().get(index) + "` = " + (arg == null ? "NULL" : sqlParam.next(arg)) + " ");
            sql.append("WHERE ");
            sql.append("`" + entityMapper.primaryKeyColumn() + "` = " + sqlParam.next(primaryKeyValue) + " ");
            sqlExecuter.update(connGetter, connCloser, sql.toString(), sqlParam.params);
        });
    }

    @Override
    public E getOne() {
        E e = super.getOne();
        if (e != null && inTransaction) {
            StringBuilder sql2 = new StringBuilder("");
            SqlParam sqlParam2 = new SqlParam();
            Object primaryKeyValue = null;
            try {
                primaryKeyValue = entityMapper.primaryKeyGetter().invoke(e);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException("Primary key is not found " + e, ex);
            }
            if (primaryKeyValue == null) {
                throw new RuntimeException("Primary key is null " + e);
            }
            sql2.append("SELECT ");
            sql2.append(entityMapper.columns().stream().map(column -> "`" + column + "`").collect(Collectors.joining(",")) + " ");
            sql2.append("FROM ");
            sql2.append("`" + entityMapper.tableName() + "` ");
            sql2.append("WHERE `" + entityMapper.primaryKeyColumn() + "`=" + sqlParam2.next(primaryKeyValue) + " ");
            sql2.append("FOR UPDATE ");
            e = sqlExecuter.selectOne(connGetter, connCloser, sql2.toString(), sqlParam2.params, c);
        }
        return entityProxyFactory.build(e);
    }

    @Override
    public List<E> getAll() {
        List<E> eList = super.getAll();
        return entityProxyFactory.build(eList);
    }

    @Override
    public List<E> get(long offset, long limit) {
        List<E> eList = super.get(offset, limit);
        return entityProxyFactory.build(eList);
    }

    @Override
    public E add(E e) {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        Map<String, Object> params = new LinkedHashMap<>();
        try {
            for (int i = 0; i < entityMapper.columns().size(); i++) {
                Method getter = entityMapper.getters().get(i);
                if (getter != null) {
                    Object paramValue = getter.invoke(e);
                    if (paramValue != null) {
                        params.put(entityMapper.columns().get(i), paramValue);
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
        sql.append("INSERT INTO ");
        sql.append("`" + entityMapper.tableName() + "` ");
        sql.append("(" + params.keySet().stream().map(column -> "`" + column + "`").collect(Collectors.joining(",")) + ") ");
        sql.append("VALUES ");
        sql.append("(" + params.values().stream().map(value -> sqlParam.next(value)).collect(Collectors.joining(",")) + ") ");
        Object primaryKeyValue = null;
        if (entityMapper.autoPrimaryKey()) {
            primaryKeyValue = sqlExecuter.insert(connGetter, connCloser, sql.toString(), sqlParam.params, c);
        } else {
            try {
                primaryKeyValue = entityMapper.primaryKeyGetter().invoke(e);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException("Primary key is not found " + e, ex);
            }
            if (primaryKeyValue == null) {
                throw new RuntimeException("Primary key can not be NULL " + e);
            }
            sqlExecuter.update(connGetter, connCloser, sql.toString(), sqlParam.params);
        }

        StringBuilder sql2 = new StringBuilder("");
        SqlParam sqlParam2 = new SqlParam();
        sql2.append("SELECT ");
        sql2.append(entityMapper.columns().stream().map(column -> "`" + column + "`").collect(Collectors.joining(",")) + " ");
        sql2.append("FROM ");
        sql2.append("`" + entityMapper.tableName() + "` ");
        sql2.append("WHERE `" + entityMapper.primaryKeyColumn() + "`=" + sqlParam2.next(primaryKeyValue) + " ");
        e = sqlExecuter.selectOne(connGetter, connCloser, sql2.toString(), sqlParam2.params, c);
        return entityProxyFactory.build(e);
    }

    @Override
    public long mixById(E e) {
        Object primaryKeyValue = null;
        try {
            primaryKeyValue = entityMapper.primaryKeyGetter().invoke(e);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException("Primary key is not found " + e, ex);
        }
        if (primaryKeyValue == null) {
            throw new RuntimeException("Primary key can not be NULL " + e);
        }
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        Map<String, Object> params = new LinkedHashMap<>();
        try {
            for (int i = 0; i < entityMapper.columns().size(); i++) {
                Method getter = entityMapper.getters().get(i);
                if (getter != null && !entityMapper.columns().get(i).equals(entityMapper.primaryKeyColumn())) {
                    Object paramValue = getter.invoke(e);
                    if (paramValue != null) {
                        params.put(entityMapper.columns().get(i), paramValue);
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
        if (params.size() == 0) {
            return 0L;
        }
        sql.append("UPDATE ");
        sql.append("`" + entityMapper.tableName() + "` ");
        sql.append("SET ");
        sql.append(params.keySet().stream().map(column -> "`" + column + "`=" + sqlParam.next(params.get(column))).collect(Collectors.joining(",")) + " ");
        sql.append("WHERE `" + entityMapper.primaryKeyColumn() + "`=" + sqlParam.next(primaryKeyValue) + " ");
        return sqlExecuter.update(connGetter, connCloser, sql.toString(), sqlParam.params);
    }

    @Override
    public long replaceById(E e) {
        Object primaryKeyValue = null;
        try {
            primaryKeyValue = entityMapper.primaryKeyGetter().invoke(e);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException("Primary key is not found " + e, ex);
        }
        if (primaryKeyValue == null) {
            throw new RuntimeException("Primary key can not be NULL " + e);
        }
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        Map<String, Object> params = new LinkedHashMap<>();
        try {
            for (int i = 0; i < entityMapper.columns().size(); i++) {
                Method getter = entityMapper.getters().get(i);
                if (getter != null && !entityMapper.columns().get(i).equals(entityMapper.primaryKeyColumn())) {
                    Object paramValue = getter.invoke(e);
                    params.put(entityMapper.columns().get(i), paramValue);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
        if (params.size() == 0) {
            return 0L;
        }
        sql.append("UPDATE ");
        sql.append("`" + entityMapper.tableName() + "` ");
        sql.append("SET ");
        sql.append(params.keySet().stream().map(column -> "`" + column + "`=" + (params.get(column) == null ? "NULL" : sqlParam.next(params.get(column)))).collect(Collectors.joining(",")) + " ");
        sql.append("WHERE `" + entityMapper.primaryKeyColumn() + "`=" + sqlParam.next(primaryKeyValue) + " ");
        return sqlExecuter.update(connGetter, connCloser, sql.toString(), sqlParam.params);
    }

    @Override
    public long mixAll(E e) {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        Map<String, Object> params = new LinkedHashMap<>();
        try {
            for (int i = 0; i < entityMapper.columns().size(); i++) {
                Method getter = entityMapper.getters().get(i);
                if (getter != null && !entityMapper.columns().get(i).equals(entityMapper.primaryKeyColumn())) {
                    Object paramValue = getter.invoke(e);
                    if (paramValue != null) {
                        params.put(entityMapper.columns().get(i), paramValue);
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
        if (params.size() == 0) {
            return 0L;
        }
        sql.append("UPDATE ");
        sql.append("`" + entityMapper.tableName() + "` ");
        sql.append("SET ");
        sql.append(params.keySet().stream().map(column -> "`" + column + "`=" + sqlParam.next(params.get(column))).collect(Collectors.joining(",")) + " ");
        if (filter != null) {
            sql.append("WHERE ");
            sql.append(filter2Sql(filter, sqlParam));
        }
        return sqlExecuter.update(connGetter, connCloser, sql.toString(), sqlParam.params);
    }

    @Override
    public long replaceAll(E e) {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < entityMapper.columns().size(); i++) {
            Method getter = entityMapper.getters().get(i);
            if (getter != null && !entityMapper.columns().get(i).equals(entityMapper.primaryKeyColumn())) {
                try {
                    Object paramValue = getter.invoke(e);
                    params.put(entityMapper.columns().get(i), paramValue);
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        if (params.size() == 0) {
            return 0L;
        }
        sql.append("UPDATE ");
        sql.append("`" + entityMapper.tableName() + "` ");
        sql.append("SET ");
        sql.append(params.keySet().stream().map(column -> "`" + column + "`=" + (params.get(column) == null ? "NULL" : sqlParam.next(params.get(column)))).collect(Collectors.joining(",")) + " ");
        if (filter != null) {
            sql.append("WHERE ");
            sql.append(filter2Sql(filter, sqlParam));
        }
        return sqlExecuter.update(connGetter, connCloser, sql.toString(), sqlParam.params);
    }

    @Override
    public long removeById(E e) {
        Object primaryKeyValue = null;
        try {
            primaryKeyValue = entityMapper.primaryKeyGetter().invoke(e);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException("Primary key is not found " + e, ex);
        }
        if (primaryKeyValue == null) {
            throw new RuntimeException("Primary key can not be NULL " + e);
        }
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("DELETE FROM ");
        sql.append("`" + entityMapper.tableName() + "` ");
        sql.append("WHERE ");
        sql.append("`" + entityMapper.primaryKeyColumn() + "`=" + sqlParam.next(primaryKeyValue) + " ");
        return sqlExecuter.update(connGetter, connCloser, sql.toString(), sqlParam.params);
    }

    @Override
    public long removeAll() {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("DELETE FROM ");
        sql.append("`" + entityMapper.tableName() + "` ");
        if (filter != null) {
            sql.append("WHERE ");
            sql.append(filter2Sql(filter, sqlParam));
        }
        return sqlExecuter.update(connGetter, connCloser, sql.toString(), sqlParam.params);
    }

    @Override
    public <R> R sum(Function<E, R> field, Class<R> c) {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT SUM(`" + entityMapper.getColumnByFunction(field) + "`) ");
        sql.append("FROM `" + entityMapper.tableName() + "` ");
        if (filter != null) {
            sql.append("WHERE ");
            sql.append(filter2Sql(filter, sqlParam));
        }
        return sqlExecuter.selectColumn(connGetter, connCloser, sql.toString(), sqlParam.params, c);
    }

    @Override
    public <R> R avg(Function<E, R> field, Class<R> c) {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT AVG(`" + entityMapper.getColumnByFunction(field) + "`) ");
        sql.append("FROM `" + entityMapper.tableName() + "` ");
        if (filter != null) {
            sql.append("WHERE ");
            sql.append(filter2Sql(filter, sqlParam));
        }
        return sqlExecuter.selectColumn(connGetter, connCloser, sql.toString(), sqlParam.params, c);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public PignooWriter<E> sort(Function<E, ?> field, PignooSorter.SMode mode) {
        super.sort(field, mode);
        return this;
    }

    @Override
    public PignooWriter<E> sort(PignooSorter<E> sorter) {
        super.sort(sorter);
        return this;
    }

    @Override
    public PignooWriter<E> filter(Function<E, ?> field, PignooFilter.FMode mode, Object... values) {
        super.filter(field, mode, values);
        return this;
    }

    @Override
    public PignooWriter<E> filter(Function<E, ?> field, String mode, Object... values) {
        super.filter(field, mode, values);
        return this;
    }

    @Override
    public PignooWriter<E> filter(PignooFilter<E> filter) {
        super.filter(filter);
        return this;
    }

    @Override
    public PignooWriter<E> filter(Function<PignooFilter<E>, PignooFilter<E>> filterBuilder) {
        super.filter(filterBuilder);
        return this;
    }
}
