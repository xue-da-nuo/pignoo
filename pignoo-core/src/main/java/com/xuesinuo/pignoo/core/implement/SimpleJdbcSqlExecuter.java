package com.xuesinuo.pignoo.core.implement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.xuesinuo.pignoo.core.SqlExecuter;
import com.xuesinuo.pignoo.core.entity.EntityMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 原生JDBC的{@link com.xuesinuo.pignoo.core.SqlExecuter}实现
 * <p>
 * Naive JDBC {@link com.xuesinuo.pignoo.core.SqlExecuter} implementation
 *
 * @author xuesinuo
 * @since 0.1.0
 * @version 0.1.0
 */
@Slf4j
public class SimpleJdbcSqlExecuter implements SqlExecuter {

    private SimpleJdbcSqlExecuter() {}

    private static final SimpleJdbcSqlExecuter instance = new SimpleJdbcSqlExecuter();

    /**
     * 单例的实现
     * <p>
     * Singleton implementation
     *
     * @return SQL执行器
     *         <p>
     *         SQL executer
     */
    public static SimpleJdbcSqlExecuter getInstance() {
        return instance;
    }

    /** {@inheritDoc} */
    @Override
    public <E> E selectOne(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params, Class<E> c) {
        log.debug(sql);
        log.debug(params.toString());
        EntityMapper<E> mapper = EntityMapper.build(c);
        Connection conn = null;
        try {
            conn = connGetter.get();
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
            }
        } catch (Exception e) {
            RuntimeException ex;
            if (e instanceof RuntimeException) {
                ex = (RuntimeException) e;
            } else {
                ex = new RuntimeException(e);
            }
            throw ex;
        } finally {
            if (conn != null) {
                connCloser.accept(conn);
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public <E> List<E> selectList(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params, Class<E> c) {
        log.debug(sql);
        log.debug(params.toString());
        EntityMapper<E> mapper = EntityMapper.build(c);
        ArrayList<E> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = connGetter.get();
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
            }
        } catch (Exception e) {
            RuntimeException ex;
            if (e instanceof RuntimeException) {
                ex = (RuntimeException) e;
            } else {
                ex = new RuntimeException(e);
            }
            throw ex;
        } finally {
            if (conn != null) {
                connCloser.accept(conn);
            }
        }
        return list;
    }

    /** {@inheritDoc} */
    @Override
    public List<LinkedHashMap<String, String>> selectLinkedHashMap(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params) {
        log.debug(sql);
        log.debug(params.toString());
        List<LinkedHashMap<String, String>> result = new ArrayList<>();
        Connection conn = null;
        try {
            conn = connGetter.get();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                    ps.setObject(entry.getKey() + 1, entry.getValue());
                }
                try (ResultSet rs = ps.executeQuery()) {
                    List<String> columnNames = new ArrayList<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        columnNames.add(rs.getMetaData().getColumnName(i));
                    }
                    while (rs.next()) {
                        LinkedHashMap<String, String> row = new LinkedHashMap<>();
                        for (String columnName : columnNames) {
                            row.put(columnName, rs.getString(columnName));
                        }
                        result.add(row);
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
        } finally {
            if (conn != null) {
                connCloser.accept(conn);
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public <R> R selectColumn(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params, Class<R> c) {
        log.debug(sql);
        log.debug(params.toString());
        Connection conn = null;
        try {
            conn = connGetter.get();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                    ps.setObject(entry.getKey() + 1, entry.getValue());
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        return rs.getObject(1, c);
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
        } finally {
            if (conn != null) {
                connCloser.accept(conn);
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public <R> Object insert(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params, Class<R> c) {
        log.debug(sql);
        log.debug(params.toString());
        Object primaryKeyValue = null;
        Connection conn = null;
        try {
            conn = connGetter.get();
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
            }
        } catch (Exception e) {
            RuntimeException ex;
            if (e instanceof RuntimeException) {
                ex = (RuntimeException) e;
            } else {
                ex = new RuntimeException(e);
            }
            throw ex;
        } finally {
            if (conn != null) {
                connCloser.accept(conn);
            }
        }
        return primaryKeyValue;
    }

    /** {@inheritDoc} */
    @Override
    public long update(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params) {
        log.debug(sql);
        log.debug(params.toString());
        Connection conn = null;
        try {
            conn = connGetter.get();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                    ps.setObject(entry.getKey() + 1, entry.getValue());
                }
                int rowsAffected = ps.executeUpdate();
                return rowsAffected;
            }
        } catch (Exception e) {
            RuntimeException ex;
            if (e instanceof RuntimeException) {
                ex = (RuntimeException) e;
            } else {
                ex = new RuntimeException(e);
            }
            throw ex;
        } finally {
            if (conn != null) {
                connCloser.accept(conn);
            }
        }
    }

}
