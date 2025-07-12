
package com.xuesinuo.pignoo.implement;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.xuesinuo.pignoo.Pignoo;
import com.xuesinuo.pignoo.PignooFilter;
import com.xuesinuo.pignoo.PignooList;
import com.xuesinuo.pignoo.PignooSorter;
import com.xuesinuo.pignoo.SqlExecuter;
import com.xuesinuo.pignoo.PignooFilter.XOR;
import com.xuesinuo.pignoo.entity.EntityMapper;
import com.xuesinuo.pignoo.entity.EntityProxyFactory;

public class MySqlPignooList<E> implements PignooList<E> {

    private static final SqlExecuter sqlExecute = new SimpleJdbcSqlExecuter();

    private final Pignoo pignoo;
    private final Connection conn;
    private final boolean useJdbcTransaction;
    private final Class<E> c;
    private final EntityMapper<E> entityMapper;
    private PignooFilter<E> filter;
    private PignooSorter<E> sorter;
    private final EntityProxyFactory<E> entityProxyFactory;

    public MySqlPignooList(Pignoo pignoo, Connection conn, boolean useJdbcTransaction, Class<E> c) {
        this.pignoo = pignoo;
        this.conn = conn;
        this.useJdbcTransaction = useJdbcTransaction;
        this.c = c;
        this.entityMapper = new EntityMapper<>(c);
        this.entityProxyFactory = new EntityProxyFactory<>(c, entityMapper, (index, arg, pig) -> {
            if (pignoo.getHasClosed()) {
                return;
            }
            StringBuilder sql = new StringBuilder("");
            sql.append("UPDATE ");
            sql.append("`" + entityMapper.tableName() + "` ");
            sql.append("SET ");
            sql.append("`" + entityMapper.columns().get(index) + "` = ? ");
            sql.append("WHERE ");
            sql.append("`" + entityMapper.primaryKeyColumn() + "` = ? ");
            try {
                sqlExecute.update(conn, sql.toString(), Map.of(0, arg, 1, entityMapper.primaryKeyGetter().invoke(pig)));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public PignooList<E> copy() {
        MySqlPignooList<E> pignooList = new MySqlPignooList<>(pignoo, conn, useJdbcTransaction, c);
        pignooList.filter = PignooFilter.copy(filter);
        pignooList.sorter = PignooSorter.copy(sorter);
        return pignooList;
    }

    private static class SqlParam {
        private int index = 0;
        private Map<Integer, Object> params = new LinkedHashMap<>();

        private String next(Object value) {
            params.put(index++, value);
            return "?";
        }
    }

    private String filter2Sql(PignooFilter<E> filter, SqlParam sqlParam) {
        String sql = "";
        if (filter != null) {
            if (filter.getField() != null && filter.getMode() != null) {
                sql += "`" + entityMapper.getColumnByFunction(filter.getField()) + "` " + filter.getMode().getSql() + " ";
                if (filter.getValues().size() == 1) {
                    sql += sqlParam.next(filter.getValues().iterator().next()) + " ";
                } else if (filter.getValues().size() > 1) {
                    sql += "(" + filter.getValues().stream().map(p -> sqlParam.next(p)).collect(Collectors.joining(",")) + ") ";
                }
            }
            if (filter.getXor() != null && filter.getOtherPignooFilterList() != null) {
                List<String> sqlList = filter.getOtherPignooFilterList().stream().map(f -> filter2Sql(f, sqlParam)).toList();
                sqlList.add(sql);
                sql = sqlList.stream().filter(s -> s != null && !s.isBlank()).collect(Collectors.joining(filter.getXor().getSql() + " "));
                if (filter.getXor() == XOR.OR) {
                    sql = "(" + sql + ")";
                }
            }
        }
        return sql;
    }

    private String sorter2Sql(PignooSorter<E> sorter) {
        StringBuilder sql = new StringBuilder("");
        if (sorter != null) {
            sql.append("`" + entityMapper.getColumnByFunction(sorter.getField()) + "` " + sorter.getMode().getSql() + " ");
            if (sorter.getOtherPignooSorter() != null && sorter.getOtherPignooSorter().getMode() != null) {
                String otherSql = sorter2Sql(sorter.getOtherPignooSorter());
                sql.append("," + otherSql);
            }
        }
        return sql.toString();
    }

    @Override
    public E getOne() {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT ");
        sql.append(entityMapper.columns().stream().map(column -> "`" + column + "`").collect(Collectors.joining(",")) + " ");
        sql.append("FROM ");
        sql.append("`" + entityMapper.tableName() + "` ");
        if (filter != null) {
            sql.append("WHERE ");
            sql.append(filter2Sql(filter, sqlParam));
        }
        if (sorter != null) {
            sql.append("ORDER BY ");
            sql.append(sorter2Sql(sorter));
        }
        if (useJdbcTransaction) {
            sql.append("FOR UPDATE ");
        }
        E e = sqlExecute.selectOne(conn, sql.toString(), sqlParam.params, c, entityMapper);
        return entityProxyFactory.build(e);
    }

    @Override
    public List<E> getAll() {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT ");
        sql.append(entityMapper.columns().stream().map(column -> "`" + column + "`").collect(Collectors.joining(",")) + " ");
        sql.append("FROM ");
        sql.append("`" + entityMapper.tableName() + "` ");
        if (filter != null) {
            sql.append("WHERE ");
            sql.append(filter2Sql(filter, sqlParam));
        }
        if (sorter != null) {
            sql.append("ORDER BY ");
            sql.append(sorter2Sql(sorter));
        }
        if (useJdbcTransaction) {
            sql.append("FOR UPDATE ");
        }
        List<E> eList = sqlExecute.selectList(conn, sql.toString(), sqlParam.params, c, entityMapper);
        return entityProxyFactory.build(eList);
    }

    @Override
    public List<E> get(long limit, long offset) {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT ");
        sql.append(entityMapper.columns().stream().map(column -> "`" + column + "`").collect(Collectors.joining(",")) + " ");
        sql.append("FROM ");
        sql.append("`" + entityMapper.tableName() + "` ");
        if (filter != null) {
            sql.append("WHERE ");
            sql.append(filter2Sql(filter, sqlParam));
        }
        if (sorter != null) {
            sql.append("ORDER BY ");
            sql.append(sorter2Sql(sorter));
        }
        sql.append("LIMIT " + offset + "," + limit + " ");
        if (useJdbcTransaction) {
            sql.append("FOR UPDATE ");
        }
        List<E> eList = sqlExecute.selectList(conn, sql.toString(), sqlParam.params, c, entityMapper);
        return entityProxyFactory.build(eList);
    }

    @Override
    public long size() {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT COUNT(*) FROM ");
        sql.append("`" + entityMapper.tableName() + "` ");
        if (filter != null) {
            sql.append("WHERE ");
            sql.append(filter2Sql(filter, sqlParam));
        }
        Long size = sqlExecute.selectColumn(conn, sql.toString(), sqlParam.params, Long.class);
        return size == null ? 0L : size;
    }

    @Override
    public PignooList<E> sort(Function<E, ?> field, PignooSorter.SMode mode) {
        if (this.sorter == null) {
            this.sorter = PignooSorter.build(field, mode);
        } else {
            this.sorter = this.sorter.then(field, mode);
        }
        return this;
    }

    @Override
    public PignooList<E> sort(PignooSorter<E> sorter) {
        if (this.sorter == null) {
            this.sorter = sorter;
        } else {
            this.sorter = this.sorter.then(sorter);
        }
        return this;
    }

    @Override
    public PignooList<E> filter(Function<E, ?> field, PignooFilter.FMode mode, Collection<Object> values) {
        if (this.filter == null) {
            this.filter = PignooFilter.build(field, mode, values);
        } else {
            this.filter = this.filter.and(field, mode, values);
        }
        return this;
    }

    @Override
    public PignooList<E> filter(Function<E, ?> field, PignooFilter.FMode mode, Object... values) {
        if (this.filter == null) {
            this.filter = PignooFilter.build(field, mode, values);
        } else {
            this.filter = this.filter.and(field, mode, values);
        }
        return this;
    }

    @Override
    public PignooList<E> filter(PignooFilter<E> filter) {
        if (this.filter == null) {
            this.filter = filter;
        } else {
            this.filter = this.filter.and(filter);
        }
        return this;
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
        Object primaryKeyValue = sqlExecute.insert(conn, sql.toString(), sqlParam.params, c);

        StringBuilder sql2 = new StringBuilder("");
        SqlParam sqlParam2 = new SqlParam();
        sql2.append("SELECT ");
        sql2.append(entityMapper.columns().stream().map(column -> "`" + column + "`").collect(Collectors.joining(",")) + " ");
        sql2.append("FROM ");
        sql2.append("`" + entityMapper.tableName() + "` ");
        sql2.append("WHERE `" + entityMapper.primaryKeyColumn() + "`=" + sqlParam2.next(primaryKeyValue) + " ");
        e = sqlExecute.selectOne(conn, sql2.toString(), sqlParam2.params, c, entityMapper);
        return entityProxyFactory.build(e);
    }

    @Override
    public long mixByPk(E e) {
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
        return sqlExecute.update(conn, sql.toString(), sqlParam.params);
    }

    @Override
    public long replaceByPk(E e) {
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
        sql.append(params.keySet().stream().map(column -> "`" + column + "`=" + params.get(column) == null ? "NULL" : sqlParam.next(params.get(column))).collect(Collectors.joining(",")) + " ");
        sql.append("WHERE `" + entityMapper.primaryKeyColumn() + "`=" + sqlParam.next(primaryKeyValue) + " ");
        return sqlExecute.update(conn, sql.toString(), sqlParam.params);
    }

    @Override
    public long mix(E e) {
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
        return sqlExecute.update(conn, sql.toString(), sqlParam.params);
    }

    @Override
    public long replace(E e) {
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
        sql.append(params.keySet().stream().map(column -> "`" + column + "`=" + params.get(column) == null ? "NULL" : sqlParam.next(params.get(column))).collect(Collectors.joining(",")) + " ");
        if (filter != null) {
            sql.append("WHERE ");
            sql.append(filter2Sql(filter, sqlParam));
        }
        return sqlExecute.update(conn, sql.toString(), sqlParam.params);
    }

    @Override
    public long removeByPk(E e) {
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
        return sqlExecute.update(conn, sql.toString(), sqlParam.params);
    }

    @Override
    public long remove() {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("DELETE FROM ");
        sql.append("`" + entityMapper.tableName() + "` ");
        if (filter != null) {
            sql.append("WHERE ");
            sql.append(filter2Sql(filter, sqlParam));
        }
        return sqlExecute.update(conn, sql.toString(), sqlParam.params);
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
        return sqlExecute.selectColumn(conn, sql.toString(), sqlParam.params, c);
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
        return sqlExecute.selectColumn(conn, sql.toString(), sqlParam.params, c);
    }
}
