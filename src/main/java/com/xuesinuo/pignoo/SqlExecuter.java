package com.xuesinuo.pignoo;

import java.util.List;
import java.util.Map;

import com.xuesinuo.pignoo.entity.EntityMapper;

public interface SqlExecuter {

    <E> E selectOne(String sql, Map<Integer, Object> params, Class<E> c, EntityMapper<E> mapper);

    <E> List<E> selectList(String sql, Map<Integer, Object> params, Class<E> c, EntityMapper<E> mapper);

    <E> long selectCount(String sql, Map<Integer, Object> params, EntityMapper<E> entityMapper);

    <E> Object insert(String sql, Map<Integer, Object> params, Class<E> c, EntityMapper<E> mapper);

    long update(String sql, Map<Integer, Object> params);
}
