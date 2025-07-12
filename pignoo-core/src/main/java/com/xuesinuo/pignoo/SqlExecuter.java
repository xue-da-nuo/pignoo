package com.xuesinuo.pignoo;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public interface SqlExecuter {

    <E> E selectOne(Connection conn, String sql, Map<Integer, Object> params, Class<E> c);

    <E> List<E> selectList(Connection conn, String sql, Map<Integer, Object> params, Class<E> c);

    <R> R selectColumn(Connection conn, String sql, Map<Integer, Object> params, Class<R> c);

    <R> Object insert(Connection conn, String sql, Map<Integer, Object> params, Class<R> c);

    long update(Connection conn, String sql, Map<Integer, Object> params);
}
