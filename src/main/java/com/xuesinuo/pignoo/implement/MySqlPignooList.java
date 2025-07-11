
package com.xuesinuo.pignoo.implement;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.util.unit.DataSize;

import com.xuesinuo.pignoo.PignooFilter;
import com.xuesinuo.pignoo.PignooList;
import com.xuesinuo.pignoo.PignooSorter;
import com.xuesinuo.pignoo.SqlExecuter;
import com.xuesinuo.pignoo.PignooFilter.XOR;
import com.xuesinuo.pignoo.entity.EntityMapper;
import com.xuesinuo.pignoo.entity.EntityProxyFactory;

public class MySqlPignooList<E> implements PignooList<E> {

    private Class<E> c;
    private EntityMapper<E> entityMapper;
    private PignooFilter<E> filter;
    private PignooSorter<E> sorter;
    private SqlExecuter sqlExecute;
    private EntityProxyFactory<E> entityProxyFactory;

    public MySqlPignooList(DataSource dataSource, Class<E> c) {
        this.c = c;
        this.entityMapper = new EntityMapper<>(c);
        this.sqlExecute = new SimpleJdbcSqlExecuter(dataSource);
        this.entityProxyFactory = new EntityProxyFactory<>(c, entityMapper, sqlExecute);
    }

    @Override
    public PignooList<E> copy() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
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
        E e = sqlExecute.selectOne(sql.toString(), sqlParam.params, c, entityMapper);
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
        List<E> eList = sqlExecute.selectList(sql.toString(), sqlParam.params, c, entityMapper);
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
        List<E> eList = sqlExecute.selectList(sql.toString(), sqlParam.params, c, entityMapper);
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
        return sqlExecute.selectCount(sql.toString(), sqlParam.params, entityMapper);
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
        for (int i = 0; i < entityMapper.columns().size(); i++) {
            Method getter = entityMapper.getters().get(i);
            if (getter != null) {
                try {
                    Object paramValue = getter.invoke(e);
                    if (paramValue != null) {
                        params.put(entityMapper.columns().get(i), paramValue);
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }
        sql.append("INSERT INTO ");
        sql.append("`" + entityMapper.tableName() + "` ");
        sql.append("(" + params.keySet().stream().map(column -> "`" + column + "`").collect(Collectors.joining(",")) + ") ");
        sql.append("VALUES ");
        sql.append("(" + params.values().stream().map(value -> sqlParam.next(value)).collect(Collectors.joining(",")) + ") ");
        Object primaryKeyValue = sqlExecute.insert(sql.toString(), sqlParam.params, c, entityMapper);

        StringBuilder sql2 = new StringBuilder("");
        SqlParam sqlParam2 = new SqlParam();
        sql2.append("SELECT ");
        sql2.append(entityMapper.columns().stream().map(column -> "`" + column + "`").collect(Collectors.joining(",")) + " ");
        sql2.append("FROM ");
        sql2.append("`" + entityMapper.tableName() + "` ");
        sql2.append("WHERE `" + entityMapper.primaryKeyColumn() + "`=" + sqlParam2.next(primaryKeyValue) + " ");
        e = sqlExecute.selectOne(sql2.toString(), sqlParam2.params, c, entityMapper);
        return entityProxyFactory.build(e);
    }

    @Override
    public long remove(E e) {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("DELETE FROM ");
        sql.append("`" + entityMapper.tableName() + "` ");
        sql.append("WHERE ");
        try {
            sql.append("`" + entityMapper.primaryKeyColumn() + "`=" + sqlParam.next(entityMapper.primaryKeyGetter().invoke(e)) + " ");
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new RuntimeException("Primary key is not found " + e);
        }
        return sqlExecute.update(sql.toString(), sqlParam.params);
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
        return sqlExecute.update(sql.toString(), sqlParam.params);
    }

}
