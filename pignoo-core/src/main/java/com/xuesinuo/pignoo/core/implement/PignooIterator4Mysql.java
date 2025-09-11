package com.xuesinuo.pignoo.core.implement;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import com.xuesinuo.pignoo.core.PignooSorter.SMode;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.SqlExecuter;
import com.xuesinuo.pignoo.core.entity.SqlParam;
import com.xuesinuo.pignoo.core.exception.MapperException;

/**
 * MySQL的集合遍历器
 * <p>
 * MySQL Iterator
 * 
 * @author xuesinuo
 * @version 1.1.0
 * @since 1.1.3
 */
public class PignooIterator4Mysql<E> implements Iterator<E> {
    /**
     * SQL执行器
     * <p>
     * SQL Executer
     */
    private static final SqlExecuter sqlExecuter = SimpleJdbcSqlExecuter.getInstance();

    private final PignooReader4Mysql<E> reader;
    private final PignooWriter4Mysql<E> writer;
    private final PignooConfig config;
    private final Class<E> c;
    private final boolean isReadOnly;
    private final int step;
    private final long offset;
    private final long limit;
    private final SMode idSortMode;

    /** 当前从数据库取出的缓存list */
    private List<E> list;
    /** 当前页数，从1开始 */
    private long pageIndex = 0;
    /** 当前页序号，从0开始 */
    private int stepIndex = -1;
    /** 总序号，从0开始 */
    private long index = 0;
    /** 当前元素 */
    private E now;

    /**
     * MySQL遍历器的构造方法
     * 
     * @param reader     被遍历的集合
     * @param c          元素类型
     * @param isReadOnly 是否只读
     * @param step       每次读取的步长
     * @param offset     起始偏移量
     * @param limit      限制总数量
     * @param idSortMode 遍历顺序，不可NULL
     */
    public PignooIterator4Mysql(PignooReader4Mysql<E> reader, Class<E> c, boolean isReadOnly, int step, long offset, long limit, SMode idSortMode) {
        this.reader = reader.copyReader();
        this.writer = reader.copyWriter();
        this.config = reader.config;
        this.c = c;
        this.isReadOnly = isReadOnly;
        this.step = step;
        this.offset = offset;
        this.limit = limit;
        this.idSortMode = idSortMode;
        try {
            this.nextStep();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new MapperException("Get primary key failed in iterator", e);
        }
    }

    /**
     * 加载下一页到list
     * 
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void nextStep() throws IllegalAccessException, InvocationTargetException {
        this.pageIndex++;
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT ");
        sql.append(this.reader.entityMapper.columns().stream().map(column -> "`" + column + "`").collect(Collectors.joining(",")) + " ");
        sql.append("FROM ");
        sql.append("`" + this.reader.entityMapper.tableName() + "` ");
        String sqlWhere = "";
        if (this.pageIndex > 1) {
            sqlWhere += "`" + this.reader.entityMapper.primaryKeyColumn() + "` ";
            if (this.idSortMode == SMode.MIN_FIRST) {
                sqlWhere += "> ";
            } else {
                sqlWhere += "< ";
            }
            try {
                Object lastPk = this.reader.entityMapper.primaryKeyGetter().run(list.getLast());
                sqlWhere += sqlParam.next(lastPk) + " ";
            } catch (Throwable throwable) {
                throw new MapperException("Get primary key failed in iterator", throwable);
            }
        }
        if (this.reader.filter != null) {
            String thisWhere = this.reader.filter2Sql(this.reader.filter, sqlParam);
            if (thisWhere != null && !thisWhere.isBlank()) {
                if (!sqlWhere.isBlank()) {
                    sqlWhere += "AND ";
                }
                sqlWhere += thisWhere;
            }
        }
        if (!sqlWhere.isBlank()) {
            sql.append("WHERE ").append(sqlWhere);
        }
        sql.append("ORDER BY ");
        sql.append("`" + this.reader.entityMapper.primaryKeyColumn() + "` " + this.reader.smodeToSql(this.idSortMode) + " ");
        if (this.pageIndex == 1) {
            sql.append("LIMIT " + this.offset + "," + (this.step + 1) + " ");
        } else {
            sql.append("LIMIT " + this.step + " ");
        }
        this.list = sqlExecuter.selectList(this.reader.connGetter, this.reader.connCloser, sql.toString(), sqlParam.params, this.c, config);
        this.stepIndex = 0;
    }

    @Override
    public synchronized boolean hasNext() {
        if (list.isEmpty()) {
            return false;
        }
        if (this.pageIndex == 1 && this.stepIndex >= this.list.size()) {
            return false;
        }
        if (this.pageIndex > 1 && this.stepIndex > this.list.size()) {
            return false;
        }
        if (this.index >= this.limit) {
            return false;
        }
        return true;
    }

    @Override
    public synchronized E next() {
        if (this.hasNext() == false) {
            throw new NoSuchElementException("No more data in database");
        }
        this.index++;
        E entity;
        if (this.stepIndex + 1 >= list.size()) {
            entity = list.getLast();
            try {
                this.nextStep();
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new MapperException("Get primary key failed in iterator", e);
            }
        } else {
            entity = list.get(stepIndex);
            this.stepIndex++;
        }
        this.now = entity;
        if (this.isReadOnly == false) {
            if (this.writer.entityProxyFactory != null) {
                entity = this.writer.entityProxyFactory.build(entity);
            }
        }
        return entity;
    }

    @Override
    public void remove() {
        if (this.isReadOnly) {
            throw new UnsupportedOperationException("remove");
        }
        if (this.now == null) {
            throw new IllegalStateException("Iterator got a null value, please call next() first");
        }
        writer.removeById(this.now);
    }
}
