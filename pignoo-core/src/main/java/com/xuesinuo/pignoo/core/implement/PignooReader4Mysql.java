
package com.xuesinuo.pignoo.core.implement;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooFilter;
import com.xuesinuo.pignoo.core.PignooReader;
import com.xuesinuo.pignoo.core.PignooSorter;
import com.xuesinuo.pignoo.core.SqlExecuter;
import com.xuesinuo.pignoo.core.PignooFilter.FMode;
import com.xuesinuo.pignoo.core.PignooFilter.XOR;
import com.xuesinuo.pignoo.core.PignooSorter.SMode;
import com.xuesinuo.pignoo.core.entity.EntityMapper;
import com.xuesinuo.pignoo.core.entity.SqlParam;
import com.xuesinuo.pignoo.core.exception.MapperException;

/**
 * 基于MySQL语法实现的{@link com.xuesinuo.pignoo.core.PignooReader}
 * <p>
 * A MySQL implementation of {@link com.xuesinuo.pignoo.core.PignooReader}
 *
 * @param <E> JavaBean Type
 * @author xuesinuo
 * @since 0.2.3
 * @version 1.1.0
 */
public class PignooReader4Mysql<E> implements PignooReader<E>, Iterable<E> {

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
    public PignooReader4Mysql(Pignoo pignoo, Supplier<Connection> connGetter, Consumer<Connection> connCloser, boolean inTransaction, Class<E> c, PignooConfig config) {
        this.pignoo = pignoo;
        this.inTransaction = inTransaction;
        this.connGetter = connGetter;
        this.connCloser = connCloser;
        this.c = c;
        this.config = config.copy();
        this.entityMapper = EntityMapper.build(c, config);
    }

    @Override
    public PignooWriter4Mysql<E> copyWriter() {
        PignooWriter4Mysql<E> pignooWriter = new PignooWriter4Mysql<>(pignoo, connGetter, connCloser, inTransaction, c, config);
        pignooWriter.filter = PignooFilter.copy(filter);
        pignooWriter.sorter = PignooSorter.copy(sorter);
        return pignooWriter;
    }

    @Override
    public PignooReader4Mysql<E> copyReader() {
        PignooReader4Mysql<E> pignooWriter = new PignooReader4Mysql<>(pignoo, connGetter, connCloser, inTransaction, c, config);
        pignooWriter.filter = PignooFilter.copy(filter);
        pignooWriter.sorter = PignooSorter.copy(sorter);
        return pignooWriter;
    }

    @Override
    public boolean isReadOnly() {
        return true;
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
            if (filter.getMode() == FMode.IN || filter.getMode() == FMode.NOT_IN || filter.getMode() == FMode.EQ || filter.getMode() == FMode.NE || filter.getMode() == FMode.NOT_LIKE) {
                if (filter.getValues().size() != valueCount) {
                    valueCount += 1;
                    hasNull = true;
                }
            }
            if (filter.getMode().getMinCount() > valueCount || filter.getMode().getMaxCount() < valueCount) {
                throw new MapperException(filter.getMode() + " can not use " + valueCount + " values -> " +
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
                } else if (filter.getMode() == FMode.NOT_LIKE) {
                    // DO NOTHING
                } else {
                    throw new MapperException(filter.getMode() + " can not be NULL -> " +
                            this.entityMapper.tableName() + "." + this.entityMapper.getColumnByFunction(filter.getField()));
                }
            } else {
                if (filter.getMode() == FMode.NOT_IN) {
                    sql = "(" + sql + (sql.isBlank() ? "" : "OR ") + "`" + entityMapper.getColumnByFunction(filter.getField()) + "` IS NULL)";
                }
                if (filter.getMode() == FMode.NOT_LIKE) {
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

    @Override
    public E getFirst() {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT ");
        sql.append(entityMapper.columns().stream().map(column -> "`" + column + "`").collect(Collectors.joining(",")) + " ");
        sql.append("FROM ");
        sql.append("`" + entityMapper.tableName() + "` ");
        if (filter != null) {
            String sqlWhere = filter2Sql(filter, sqlParam);
            if (sqlWhere != null && !sqlWhere.isBlank()) {
                sql.append("WHERE ");
                sql.append(sqlWhere);
            }
        }
        if (sorter != null) {
            sql.append("ORDER BY ");
            sql.append(sorter2Sql(sorter));
        }
        sql.append("LIMIT 1 ");
        E e = sqlExecuter.selectOne(connGetter, connCloser, sql.toString(), sqlParam.params, c);
        return e;
    }

    @Override
    public E getAny() {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT ");
        sql.append(entityMapper.columns().stream().map(column -> "`" + column + "`").collect(Collectors.joining(",")) + " ");
        sql.append("FROM ");
        sql.append("`" + entityMapper.tableName() + "` ");
        if (filter != null) {
            String sqlWhere = filter2Sql(filter, sqlParam);
            if (sqlWhere != null && !sqlWhere.isBlank()) {
                sql.append("WHERE ");
                sql.append(sqlWhere);
            }
        }
        sql.append("LIMIT 1 ");
        E e = sqlExecuter.selectOne(connGetter, connCloser, sql.toString(), sqlParam.params, c);
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
            String sqlWhere = filter2Sql(filter, sqlParam);
            if (sqlWhere != null && !sqlWhere.isBlank()) {
                sql.append("WHERE ");
                sql.append(sqlWhere);
            }
        }
        if (sorter != null) {
            sql.append("ORDER BY ");
            sql.append(sorter2Sql(sorter));
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
            String sqlWhere = filter2Sql(filter, sqlParam);
            if (sqlWhere != null && !sqlWhere.isBlank()) {
                sql.append("WHERE ");
                sql.append(sqlWhere);
            }
        }
        if (sorter != null) {
            sql.append("ORDER BY ");
            sql.append(sorter2Sql(sorter));
        }
        sql.append("LIMIT " + offset + "," + limit + " ");
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
            String sqlWhere = filter2Sql(filter, sqlParam);
            if (sqlWhere != null && !sqlWhere.isBlank()) {
                sql.append("WHERE ");
                sql.append(sqlWhere);
            }
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
    public PignooReader<E> filter(Boolean condition, Function<E, ?> field, PignooFilter.FMode mode, Object... values) {
        if (condition != null && condition) {
            return filter(field, mode, values);
        }
        return this;
    }

    @Override
    public PignooReader<E> filter(Boolean condition, Function<E, ?> field, String mode, Object... values) {
        if (condition != null && condition) {
            return filter(field, mode, values);
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
    public <R> R max(Function<E, R> field, Class<R> c) {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT MAX(`" + entityMapper.getColumnByFunction(field) + "`) ");
        sql.append("FROM `" + entityMapper.tableName() + "` ");
        if (filter != null) {
            String sqlWhere = filter2Sql(filter, sqlParam);
            if (sqlWhere != null && !sqlWhere.isBlank()) {
                sql.append("WHERE ");
                sql.append(sqlWhere);
            }
        }
        return sqlExecuter.selectColumn(connGetter, connCloser, sql.toString(), sqlParam.params, c);
    }

    @Override
    public <R> R maxNullAs(Function<E, R> field, Class<R> c, R nullAs) {
        if (nullAs == null) {
            throw new NullPointerException("#maxNullAs's param 'nullAs' can not be null");
        }
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT MAX(COALESCE(`" + entityMapper.getColumnByFunction(field) + "`," + sqlParam.next(nullAs) + ")) ");
        sql.append("FROM `" + entityMapper.tableName() + "` ");
        if (filter != null) {
            String sqlWhere = filter2Sql(filter, sqlParam);
            if (sqlWhere != null && !sqlWhere.isBlank()) {
                sql.append("WHERE ");
                sql.append(sqlWhere);
            }
        }
        return sqlExecuter.selectColumn(connGetter, connCloser, sql.toString(), sqlParam.params, c);
    }

    @Override
    public <R> R min(Function<E, R> field, Class<R> c) {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT MIN(`" + entityMapper.getColumnByFunction(field) + "`) ");
        sql.append("FROM `" + entityMapper.tableName() + "` ");
        if (filter != null) {
            String sqlWhere = filter2Sql(filter, sqlParam);
            if (sqlWhere != null && !sqlWhere.isBlank()) {
                sql.append("WHERE ");
                sql.append(sqlWhere);
            }
        }
        return sqlExecuter.selectColumn(connGetter, connCloser, sql.toString(), sqlParam.params, c);
    }

    @Override
    public <R> R minNullAs(Function<E, R> field, Class<R> c, R nullAs) {
        if (nullAs == null) {
            throw new NullPointerException("#minNullAs's param 'nullAs' can not be null");
        }
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT MIN(COALESCE(`" + entityMapper.getColumnByFunction(field) + "`," + sqlParam.next(nullAs) + ")) ");
        sql.append("FROM `" + entityMapper.tableName() + "` ");
        if (filter != null) {
            String sqlWhere = filter2Sql(filter, sqlParam);
            if (sqlWhere != null && !sqlWhere.isBlank()) {
                sql.append("WHERE ");
                sql.append(sqlWhere);
            }
        }
        return sqlExecuter.selectColumn(connGetter, connCloser, sql.toString(), sqlParam.params, c);
    }

    @Override
    public <R> R sum(Function<E, R> field, Class<R> c) {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT SUM(`" + entityMapper.getColumnByFunction(field) + "`) ");
        sql.append("FROM `" + entityMapper.tableName() + "` ");
        if (filter != null) {
            String sqlWhere = filter2Sql(filter, sqlParam);
            if (sqlWhere != null && !sqlWhere.isBlank()) {
                sql.append("WHERE ");
                sql.append(sqlWhere);
            }
        }
        return sqlExecuter.selectColumn(connGetter, connCloser, sql.toString(), sqlParam.params, c);
    }

    @Override
    public <R> R sumNullAs(Function<E, R> field, Class<R> c, R nullAs) {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT SUM(COALESCE(`" + entityMapper.getColumnByFunction(field) + "`," + sqlParam.next(nullAs) + ")) ");
        sql.append("FROM `" + entityMapper.tableName() + "` ");
        if (filter != null) {
            String sqlWhere = filter2Sql(filter, sqlParam);
            if (sqlWhere != null && !sqlWhere.isBlank()) {
                sql.append("WHERE ");
                sql.append(sqlWhere);
            }
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
            String sqlWhere = filter2Sql(filter, sqlParam);
            if (sqlWhere != null && !sqlWhere.isBlank()) {
                sql.append("WHERE ");
                sql.append(sqlWhere);
            }
        }
        return sqlExecuter.selectColumn(connGetter, connCloser, sql.toString(), sqlParam.params, c);
    }

    @Override
    public <R> R avgNullAs(Function<E, R> field, Class<R> c, R nullAs) {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT AVG(COALESCE(`" + entityMapper.getColumnByFunction(field) + "`," + sqlParam.next(nullAs) + ")) ");
        sql.append("FROM `" + entityMapper.tableName() + "` ");
        if (filter != null) {
            String sqlWhere = filter2Sql(filter, sqlParam);
            if (sqlWhere != null && !sqlWhere.isBlank()) {
                sql.append("WHERE ");
                sql.append(sqlWhere);
            }
        }
        return sqlExecuter.selectColumn(connGetter, connCloser, sql.toString(), sqlParam.params, c);
    }

    @Override
    public <R> long countDistinct(Function<E, R> field) {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT COUNT(DISTINCT `" + entityMapper.getColumnByFunction(field) + "`) ");
        sql.append("FROM `" + entityMapper.tableName() + "` ");
        if (filter != null) {
            String sqlWhere = filter2Sql(filter, sqlParam);
            if (sqlWhere != null && !sqlWhere.isBlank()) {
                sql.append("WHERE ");
                sql.append(sqlWhere);
            }
        }
        Long count = sqlExecuter.selectColumn(connGetter, connCloser, sql.toString(), sqlParam.params, Long.class);
        if (count == null) {
            return 0;
        }
        return count;
    }

    @Override
    public <R> long countDistinctNullAs(Function<E, R> field, R nullAs) {
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT COUNT(DISTINCT COALESCE(`" + entityMapper.getColumnByFunction(field) + "`, " + sqlParam.next(nullAs) + ")) ");
        sql.append("FROM `" + entityMapper.tableName() + "` ");
        if (filter != null) {
            String sqlWhere = filter2Sql(filter, sqlParam);
            if (sqlWhere != null && !sqlWhere.isBlank()) {
                sql.append("WHERE ");
                sql.append(sqlWhere);
            }
        }
        Long count = sqlExecuter.selectColumn(connGetter, connCloser, sql.toString(), sqlParam.params, Long.class);
        if (count == null) {
            return 0;
        }
        return count;
    }

    @Override
    public boolean containsId(E e) {
        if (e == null) {
            return false;
        }
        Object primaryKeyValue = null;
        try {
            primaryKeyValue = entityMapper.primaryKeyGetter().invoke(e);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new MapperException("Primary key is not found " + e, ex);
        }
        if (primaryKeyValue == null) {
            throw new MapperException("Primary key can not be NULL " + e);
        }

        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT COUNT(*) FROM ");
        sql.append("`" + entityMapper.tableName() + "` ");
        sql.append("WHERE `" + entityMapper.primaryKeyColumn() + "`=" + sqlParam.next(primaryKeyValue) + " ");
        if (filter != null) {
            String sqlWhere = filter2Sql(filter, sqlParam);
            if (sqlWhere != null && !sqlWhere.isBlank()) {
                sql.append("AND ");
                sql.append(sqlWhere);
            }
        }
        Long size = sqlExecuter.selectColumn(connGetter, connCloser, sql.toString(), sqlParam.params, Long.class);
        return size != null && size > 0;
    }

    @Override
    public boolean containsIds(Collection<E> collection) {
        if (collection == null || collection.isEmpty()) {
            return false;
        }
        List<Object> pkList = collection.stream().filter(e -> e != null).map(e -> {
            try {
                return entityMapper.primaryKeyGetter().invoke(e);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new MapperException("Primary key is not found " + e, ex);
            }
        }).filter(pk -> pk != null).distinct().toList();
        if (pkList.size() != collection.size()) {
            return false;
        }

        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT COUNT(*) FROM ");
        sql.append("`" + entityMapper.tableName() + "` ");
        sql.append("WHERE `" + entityMapper.primaryKeyColumn() + "` IN ( ");
        sql.append(pkList.stream().map(pk -> sqlParam.next(pk)).collect(Collectors.joining(","))).append(") ");
        if (filter != null) {
            String sqlWhere = filter2Sql(filter, sqlParam);
            if (sqlWhere != null && !sqlWhere.isBlank()) {
                sql.append("AND ");
                sql.append(sqlWhere);
            }
        }
        Long size = sqlExecuter.selectColumn(connGetter, connCloser, sql.toString(), sqlParam.params, Long.class);
        return size != null && size.intValue() == collection.size();
    }

    @Override
    public Iterator<E> iterator() {
        return new PignooIterator4Mysql<>(this, this.c, this.isReadOnly(), 100, 0, Long.MAX_VALUE, SMode.MIN_FIRST);
    }

    @Override
    public Iterator<E> iterator(int step) {
        return new PignooIterator4Mysql<>(this, this.c, this.isReadOnly(), step, 0, Long.MAX_VALUE, SMode.MIN_FIRST);
    }

    @Override
    public Iterator<E> iterator(int step, SMode idSortMode) {
        return new PignooIterator4Mysql<>(this, this.c, this.isReadOnly(), step, 0, Long.MAX_VALUE, idSortMode);
    }

    @Override
    public Iterator<E> iterator(int step, SMode idSortMode, long offset, long limit) {
        return new PignooIterator4Mysql<>(this, this.c, this.isReadOnly(), step, offset, limit, idSortMode);
    }
}
