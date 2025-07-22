
package com.xuesinuo.pignoo.core.implement;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooFilter;
import com.xuesinuo.pignoo.core.PignooWriter;
import com.xuesinuo.pignoo.core.PignooReader;
import com.xuesinuo.pignoo.core.PignooSorter;
import com.xuesinuo.pignoo.core.SqlExecuter;
import com.xuesinuo.pignoo.core.PignooFilter.FMode;
import com.xuesinuo.pignoo.core.PignooFilter.XOR;
import com.xuesinuo.pignoo.core.PignooSorter.SMode;
import com.xuesinuo.pignoo.core.entity.EntityMapper;
import com.xuesinuo.pignoo.core.entity.SqlParam;

/**
 * 基于MySQL语法实现的{@link PignooReader}
 * 
 * @author xuesinuo
 * @since 0.2.3
 */
public class MySqlPignooReadOnlyList<E> implements PignooReader<E> {

    protected static final SqlExecuter sqlExecuter = SimpleJdbcSqlExecuter.getInstance();

    protected final Pignoo pignoo;
    protected final Supplier<Connection> connGetter;
    protected final Consumer<Connection> connCloser;
    protected final boolean inTransaction;
    protected final Class<E> c;
    protected final EntityMapper<E> entityMapper;
    protected PignooFilter<E> filter;
    protected PignooSorter<E> sorter;
    protected final PignooConfig config;

    public MySqlPignooReadOnlyList(Pignoo pignoo, Supplier<Connection> connGetter, Consumer<Connection> connCloser, boolean inTransaction, Class<E> c, PignooConfig config) {
        this.pignoo = pignoo;
        this.inTransaction = inTransaction;
        this.connGetter = connGetter;
        this.connCloser = connCloser;
        this.c = c;
        this.config = config.copy();
        this.entityMapper = EntityMapper.build(c, config);
    }

    @Override
    public PignooWriter<E> copyWriter() {
        MySqlPignooWriter<E> pignooWriter = new MySqlPignooWriter<>(pignoo, connGetter, connCloser, inTransaction, c, config);
        pignooWriter.filter = PignooFilter.copy(filter);
        pignooWriter.sorter = PignooSorter.copy(sorter);
        return pignooWriter;
    }

    @Override
    public PignooReader<E> copyReader() {
        MySqlPignooReadOnlyList<E> pignooWriter = new MySqlPignooReadOnlyList<>(pignoo, connGetter, connCloser, inTransaction, c, config);
        pignooWriter.filter = PignooFilter.copy(filter);
        pignooWriter.sorter = PignooSorter.copy(sorter);
        return pignooWriter;
    }

    protected String fmodeToSql(FMode fmode) {
        switch (fmode) {
        case EQ:
            return "=";
        case NE:
            return "!=";
        case GT:
            return ">";
        case LT:
            return "<";
        case GE:
            return ">=";
        case LE:
            return "<=";
        case LIKE:
            return "LIKE";
        case NOT_LIKE:
            return "NOT LIKE";
        case IN:
            return "IN";
        case NOT_IN:
            return "NOT IN";
        case NULL:
            return "IS NULL";
        case NOT_NULL:
            return "IS NOT NULL";
        default:
            return "";
        }
    }

    protected String smodeToSql(SMode smode) {
        switch (smode) {
        case MIN_FIRST:
            return "ASC";
        case MAX_FIRST:
            return "DESC";
        default:
            return "";
        }
    }

    protected String filter2Sql(PignooFilter<E> filter, SqlParam sqlParam) {
        StringBuilder sql = new StringBuilder("");
        if (filter != null) {
            if (filter.getXor() != null && filter.getXor() == XOR.OR) {
                sql.append("(" + filter.getOtherPignooFilterList().stream().map(f -> filter2Sql(f, sqlParam))
                        .filter(s -> s != null && !s.isBlank()).collect(Collectors.joining("OR ")) + ") ");
            } else {
                boolean first = true;
                if (filter.getField() != null) {
                    sql.append(thisFilter2Sql(filter, sqlParam));
                    first = false;
                }
                for (PignooFilter<E> childFilter : filter.getOtherPignooFilterList()) {
                    String appedSql = filter2Sql(childFilter, sqlParam).trim();
                    if (appedSql != null && !appedSql.isBlank()) {
                        sql.append((first ? "" : "AND ") + appedSql + " ");
                        first = false;
                    }
                }
            }
        }
        return sql.toString();
    }

    protected String thisFilter2Sql(PignooFilter<E> filter, SqlParam sqlParam) {
        String sql = "";
        if (filter.getField() != null && filter.getMode() != null) {
            Collection<Object> values = filter.getValues().stream().filter(p -> p != null).toList();
            int valueCount = values.size();
            boolean hasNull = false;
            if (filter.getMode() == FMode.IN || filter.getMode() == FMode.NOT_IN || filter.getMode() == FMode.EQ || filter.getMode() == FMode.NE) {
                if (filter.getValues().size() != valueCount) {
                    valueCount += 1;
                    hasNull = true;
                }
            }
            if (filter.getMode().getMinCount() > valueCount || filter.getMode().getMaxCount() < valueCount) {
                throw new RuntimeException(filter.getMode() + " can not use " + valueCount + " values -> " +
                        this.entityMapper.tableName() + "." + this.entityMapper.getColumnByFunction(filter.getField()));
            }
            if (values.size() >= filter.getMode().getMinCount()) {
                sql += "`" + entityMapper.getColumnByFunction(filter.getField()) + "` " + fmodeToSql(filter.getMode()) + " ";
                String paramSql = values.stream().map(p -> sqlParam.next(p)).collect(Collectors.joining(","));
                if (!paramSql.isBlank()) {
                    if (filter.getMode() == FMode.IN || filter.getMode() == FMode.NOT_IN) {
                        paramSql = "(" + paramSql + ")";
                    }
                    sql += paramSql + " ";
                }
            }
            if (hasNull) {
                if (filter.getMode() == FMode.IN) {
                    sql = "(" + sql + (sql.isBlank() ? "" : "OR ") + "`" + entityMapper.getColumnByFunction(filter.getField()) + "` IS NULL)";
                } else if (filter.getMode() == FMode.NOT_IN) {
                    // DO NOTHING
                } else if (filter.getMode() == FMode.EQ) {
                    sql += "`" + entityMapper.getColumnByFunction(filter.getField()) + "` IS NULL ";
                } else if (filter.getMode() == FMode.NE) {
                    sql += "`" + entityMapper.getColumnByFunction(filter.getField()) + "` IS NOT NULL ";
                } else {
                    throw new RuntimeException(filter.getMode() + " can not be NULL -> " +
                            this.entityMapper.tableName() + "." + this.entityMapper.getColumnByFunction(filter.getField()));
                }
            } else {
                if (filter.getMode() == FMode.NOT_IN) {
                    sql = "(" + sql + (sql.isBlank() ? "" : "OR ") + "`" + entityMapper.getColumnByFunction(filter.getField()) + "` IS NULL)";
                }
            }
        }
        return sql;
    }

    protected String sorter2Sql(PignooSorter<E> sorter) {
        StringBuilder sql = new StringBuilder("");
        if (sorter != null) {
            sql.append("`" + entityMapper.getColumnByFunction(sorter.getField()) + "` " + smodeToSql(sorter.getMode()) + " ");
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
        sql.append("LIMIT 1 ");
        E e = sqlExecuter.selectOne(connGetter, connCloser, sql.toString(), sqlParam.params, c);
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
        return e;
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
        if (inTransaction) {
            sql.append("FOR UPDATE ");
        }
        List<E> eList = sqlExecuter.selectList(connGetter, connCloser, sql.toString(), sqlParam.params, c);
        return eList;
    }

    @Override
    public List<E> get(long offset, long limit) {
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
        if (inTransaction) {
            sql.append("FOR UPDATE ");
        }
        List<E> eList = sqlExecuter.selectList(connGetter, connCloser, sql.toString(), sqlParam.params, c);
        return eList;
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
        Long size = sqlExecuter.selectColumn(connGetter, connCloser, sql.toString(), sqlParam.params, Long.class);
        return size == null ? 0L : size;
    }

    @Override
    public PignooReader<E> sort(Function<E, ?> field, PignooSorter.SMode mode) {
        if (this.sorter == null) {
            this.sorter = PignooSorter.build(field, mode);
        } else {
            this.sorter = this.sorter.then(field, mode);
        }
        return this;
    }

    @Override
    public PignooReader<E> sort(PignooSorter<E> sorter) {
        if (this.sorter == null) {
            this.sorter = sorter;
        } else {
            this.sorter = this.sorter.then(sorter);
        }
        return this;
    }

    @Override
    public PignooReader<E> filter(Function<E, ?> field, PignooFilter.FMode mode, Object... values) {
        if (this.filter == null) {
            this.filter = PignooFilter.build(field, mode, values);
        } else {
            this.filter = this.filter.and(field, mode, values);
        }
        return this;
    }

    @Override
    public PignooReader<E> filter(Function<E, ?> field, String mode, Object... values) {
        return filter(field, FMode.getFMode(mode), values);
    }

    @Override
    public PignooReader<E> filter(PignooFilter<E> filter) {
        if (this.filter == null) {
            this.filter = filter;
        } else {
            this.filter = this.filter.and(filter);
        }
        return this;
    }

    @Override
    public PignooReader<E> filter(Function<PignooFilter<E>, PignooFilter<E>> filterBuilder) {
        PignooFilter<E> filter = new PignooFilter<>();
        filter = filterBuilder.apply(filter);
        if (filter == null) {
            return this;
        }
        this.filter(filter);
        return this;
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
        return true;
    }
}
