package com.xuesinuo.pignoo.core;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * Pignoo的SQL执行器
 * 
 * @author xuesinuo
 * @version 0.1.0
 */
public interface SqlExecuter {

    /** 查询一个实体 */
    <E> E selectOne(Connection conn, String sql, Map<Integer, Object> params, Class<E> c);

    /** 查询实体List */
    <E> List<E> selectList(Connection conn, String sql, Map<Integer, Object> params, Class<E> c);

    /** 查询一个列 */
    <R> R selectColumn(Connection conn, String sql, Map<Integer, Object> params, Class<R> c);

    /** 插入一个数据，返回主键 */
    <R> Object insert(Connection conn, String sql, Map<Integer, Object> params, Class<R> c);

    /** 执行一个非查询，返回受影响行数 */
    long update(Connection conn, String sql, Map<Integer, Object> params);
}
