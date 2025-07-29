
package com.xuesinuo.pignoo.core.implement;

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
 * 基于MySQL语法实现的{@link com.xuesinuo.pignoo.core.PignooReader}
 * <p>
 * A MySQL implementation of {@link com.xuesinuo.pignoo.core.PignooReader}
 *
 * @param <E> JavaBean Type
 * @author xuesinuo
 * @since 0.2.3
 * @version 0.2.3
 */
public class MySqlPignooReader<E> implements PignooReader<E> {

    /**
     * SQL执行器
     * <p>
     * SQL Executer
     */
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

    /**
     * 构造器
     * <p>
     * Constructor
     *
     * @param pignoo        pignoo
     * @param connGetter    获取连接函数
     *                      <p>
     *                      Connection Getter
     * @param connCloser    关闭连接函数
     *                      <p>
     *                      Connection Closer
     * @param inTransaction 是否在事务中
     *                      <p>
     *                      Whether in transaction
     * @param c             实体类型
     *                      <p>
     *                      Entity type
     * @param config        配置
     *                      <p>
     *                      Configuration
     */
    public MySqlPignooReader(Pignoo pignoo, Supplier<Connection> connGetter, Consumer<Connection> connCloser, boolean inTransaction, Class<E> c, PignooConfig config) {
        this.pignoo = pignoo;
        this.inTransaction = inTransaction;
        this.connGetter = connGetter;
        this.connCloser = connCloser;
        this.c = c;
        this.config = config.copy();
        this.entityMapper = EntityMapper.build(c, config);
    }

    /** {@inheritDoc} */
    @Override
    public PignooWriter<E> copyWriter() {
        MySqlPignooWriter<E> pignooWriter = new MySqlPignooWriter<>(pignoo, connGetter, connCloser, inTransaction, c, config);
        pignooWriter.filter = PignooFilter.copy(filter);
        pignooWriter.sorter = PignooSorter.copy(sorter);
        return pignooWriter;
    }

    /** {@inheritDoc} */
    @Override
    public PignooReader<E> copyReader() {
        MySqlPignooReader<E> pignooWriter = new MySqlPignooReader<>(pignoo, connGetter, connCloser, inTransaction, c, config);
        pignooWriter.filter = PignooFilter.copy(filter);
        pignooWriter.sorter = PignooSorter.copy(sorter);
        return pignooWriter;
    }

    /**
     * 操作符转SQL语句
     * <p>
     * Operator to SQL statement
     *
     * @param fmode 操作符枚举
     *              <p>
     *              Operator enumeration
     * @return SQL语句
     *         <p>
     *         SQL statement
     */
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
        case IS_NULL:
            return "IS NULL";
        case IS_NOT_NULL:
            return "IS NOT NULL";
        default:
            return "";
        }
    }

    /**
     * 排序方式转SQL语句
     * <p>
     * Sorter mode to SQL statement
     *
     * @param smode 排序方式
     *              <p>
     *              Sorter mode
     * @return SQL语句
     *         <p>
     *         SQL statement
     */
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

    /**
     * 过滤器转SQL语句（filter支持嵌套）
     * <p>
     * Filter to SQL statement (filter supports nesting)
     *
     * @param filter   过滤器
     *                 <p>
     *                 Filter
     * @param sqlParam 参数拼接工具
     *                 <p>
     *                 Parameter concatenation tool
     * @return SQL语句
     *         <p>
     *         SQL statement
     */
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

    /**
     * 单个过滤器转SQL语句
     * <p>
     * Single filter to SQL statement
     *
     * @param filter   单个过滤器
     *                 <p>
     *                 Single filter
     * @param sqlParam 参数拼接工具
     *                 <p>
     *                 Parameter concatenation tool
     * @return SQL语句
     *         <p>
     *         SQL statement
     */
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

    /**
     * 排序器转SQL语句
     * <p>
     * Sorter to SQL statement
     *
     * @param sorter 排序器
     *               <p>
     *               Sorter
     * @return SQL语句
     *         <p>
     *         SQL statement
     */
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

    /** {@inheritDoc} */
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
        return e;
    }

    /** {@inheritDoc} */
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
        List<E> eList = sqlExecuter.selectList(connGetter, connCloser, sql.toString(), sqlParam.params, c);
        return eList;
    }

    /** {@inheritDoc} */
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
        List<E> eList = sqlExecuter.selectList(connGetter, connCloser, sql.toString(), sqlParam.params, c);
        return eList;
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public PignooReader<E> sort(Function<E, ?> field, PignooSorter.SMode mode) {
        if (this.sorter == null) {
            this.sorter = PignooSorter.build(field, mode);
        } else {
            this.sorter = this.sorter.then(field, mode);
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public PignooReader<E> sort(PignooSorter<E> sorter) {
        if (this.sorter == null) {
            this.sorter = sorter;
        } else {
            this.sorter = this.sorter.then(sorter);
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public PignooReader<E> filter(Boolean condition, Function<E, ?> field, PignooFilter.FMode mode, Object... values) {
        if (condition != null && condition) {
            return filter(field, mode, values);
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public PignooReader<E> filter(Boolean condition, Function<E, ?> field, String mode, Object... values) {
        if (condition != null && condition) {
            return filter(field, mode, values);
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public PignooReader<E> filter(Function<E, ?> field, PignooFilter.FMode mode, Object... values) {
        if (this.filter == null) {
            this.filter = PignooFilter.build(field, mode, values);
        } else {
            this.filter = this.filter.and(field, mode, values);
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public PignooReader<E> filter(Function<E, ?> field, String mode, Object... values) {
        return filter(field, FMode.getFMode(mode), values);
    }

    /** {@inheritDoc} */
    @Override
    public PignooReader<E> filter(PignooFilter<E> filter) {
        if (this.filter == null) {
            this.filter = filter;
        } else {
            this.filter = this.filter.and(filter);
        }
        return this;
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public boolean isReadOnly() {
        return true;
    }
}
