package com.xuesinuo.pignoo;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import com.xuesinuo.pignoo.entity.EntityMapper;

public interface SqlExecuter {

    <E> E selectOne(Connection conn, String sql, Map<Integer, Object> params, Class<E> c, EntityMapper<E> mapper);

    <E> List<E> selectList(Connection conn, String sql, Map<Integer, Object> params, Class<E> c, EntityMapper<E> mapper);

    <E> long selectCount(Connection conn, String sql, Map<Integer, Object> params, EntityMapper<E> entityMapper);

    <E> Object insert(Connection conn, String sql, Map<Integer, Object> params, Class<E> c, EntityMapper<E> mapper);

    long update(Connection conn, String sql, Map<Integer, Object> params);
}
