package com.xuesinuo.pignoo.implement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.xuesinuo.pignoo.SqlExecuter;
import com.xuesinuo.pignoo.entity.EntityMapper;

public class SimpleJdbcSqlExecuter implements SqlExecuter {

    private SimpleJdbcSqlExecuter() {}

    private static final SimpleJdbcSqlExecuter instance = new SimpleJdbcSqlExecuter();

    public static SimpleJdbcSqlExecuter getInstance() {
        return instance;
    }

    @Override
    public <E> E selectOne(Connection conn, String sql, Map<Integer, Object> params, Class<E> c) {
        System.out.println(sql);
        System.out.println(params);
        EntityMapper<E> mapper = EntityMapper.build(c);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                ps.setObject(entry.getKey() + 1, entry.getValue());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    E entity = mapper.buildEntity();
                    for (int i = 0; i < mapper.columns().size(); i++) {
                        String columnName = mapper.columns().get(i);
                        Object columnValue = rs.getObject(columnName, mapper.fields().get(i).getType());
                        mapper.setters().get(i).invoke(entity, columnValue);
                    }
                    return entity;
                }
            }
        } catch (Exception e) {
            RuntimeException ex;
            if (e instanceof RuntimeException) {
                ex = (RuntimeException) e;
            } else {
                ex = new RuntimeException(e);
            }
            throw ex;
        }
        return null;
    }

    @Override
    public <E> List<E> selectList(Connection conn, String sql, Map<Integer, Object> params, Class<E> c) {
        System.out.println(sql);
        System.out.println(params);
        EntityMapper<E> mapper = EntityMapper.build(c);
        ArrayList<E> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                ps.setObject(entry.getKey() + 1, entry.getValue());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    E entity = mapper.buildEntity();
                    for (int i = 0; i < mapper.columns().size(); i++) {
                        String columnName = mapper.columns().get(i);
                        Object columnValue = rs.getObject(columnName, mapper.fields().get(i).getType());
                        mapper.setters().get(i).invoke(entity, columnValue);
                    }
                    list.add(entity);
                }
            }
        } catch (Exception e) {
            RuntimeException ex;
            if (e instanceof RuntimeException) {
                ex = (RuntimeException) e;
            } else {
                ex = new RuntimeException(e);
            }
            throw ex;
        }
        return list;
    }

    @Override
    public <R> R selectColumn(Connection conn, String sql, Map<Integer, Object> params, Class<R> c) {
        System.out.println(sql);
        System.out.println(params);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                ps.setObject(entry.getKey() + 1, entry.getValue());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    return rs.getObject(0, c);
                }
            }
        } catch (Exception e) {
            RuntimeException ex;
            if (e instanceof RuntimeException) {
                ex = (RuntimeException) e;
            } else {
                ex = new RuntimeException(e);
            }
            throw ex;
        }
        return null;
    }

    @Override
    public <R> Object insert(Connection conn, String sql, Map<Integer, Object> params, Class<R> c) {
        System.out.println(sql);
        System.out.println(params);
        Object primaryKeyValue = null;
        try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                ps.setObject(entry.getKey() + 1, entry.getValue());
            }
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        primaryKeyValue = rs.getObject(1);
                    }
                }
            }
        } catch (Exception e) {
            RuntimeException ex;
            if (e instanceof RuntimeException) {
                ex = (RuntimeException) e;
            } else {
                ex = new RuntimeException(e);
            }
            throw ex;
        }
        return primaryKeyValue;
    }

    @Override
    public long update(Connection conn, String sql, Map<Integer, Object> params) {
        System.out.println(sql);
        System.out.println(params);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                ps.setObject(entry.getKey() + 1, entry.getValue());
            }
            int rowsAffected = ps.executeUpdate();
            return rowsAffected;
        } catch (Exception e) {
            RuntimeException ex;
            if (e instanceof RuntimeException) {
                ex = (RuntimeException) e;
            } else {
                ex = new RuntimeException(e);
            }
            throw ex;
        }
    }

}
