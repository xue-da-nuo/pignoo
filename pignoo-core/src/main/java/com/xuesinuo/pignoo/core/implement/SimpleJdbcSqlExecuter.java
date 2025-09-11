package com.xuesinuo.pignoo.core.implement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.SqlExecuter;
import com.xuesinuo.pignoo.core.entity.EntityMapper;
import com.xuesinuo.pignoo.core.exception.MapperException;
import com.xuesinuo.pignoo.core.exception.PignooRuntimeException;
import com.xuesinuo.pignoo.core.exception.SqlExecuteException;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 原生JDBC的{@link com.xuesinuo.pignoo.core.SqlExecuter}实现
 * <p>
 * Naive JDBC {@link com.xuesinuo.pignoo.core.SqlExecuter} implementation
 *
 * @author xuesinuo
 * @since 0.1.0
 * @version 1.1.3
 */
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class SimpleJdbcSqlExecuter implements SqlExecuter {

    private boolean saveLog = true;

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

    @Override
    public <E> E selectOne(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params, Class<E> c, PignooConfig config) {
        long startTime = System.currentTimeMillis();
        if (saveLog) {
            log.debug(sql);
            log.debug(params.toString());
        }
        EntityMapper<E> mapper = EntityMapper.build(c, config);
        Connection conn = null;
        try {
            conn = connGetter.get();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                    setParam(ps, entry.getKey() + 1, entry.getValue());
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        E entity = mapper.buildEntity();
                        for (int i = 0; i < mapper.columns().size(); i++) {
                            String columnName = mapper.columns().get(i);
                            Object columnValue = getObject(rs, mapper.fields().get(i).getType(), null, columnName);
                            mapper.setters().get(i).run(entity, columnValue);
                        }
                        if (saveLog) {
                            log.debug("1 row(s) in " + (System.currentTimeMillis() - startTime) + " ms");
                        }
                        return entity;
                    }
                }
            }
        } catch (Throwable e) {
            throw handleException(e);
        } finally {
            if (conn != null) {
                connCloser.accept(conn);
            }
        }
        return null;
    }

    @Override
    public <E> List<E> selectList(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params, Class<E> c, PignooConfig config) {
        long startTime = System.currentTimeMillis();
        if (saveLog) {
            log.debug(sql);
            log.debug(params.toString());
        }
        EntityMapper<E> mapper = EntityMapper.build(c, config);
        ArrayList<E> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = connGetter.get();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                    setParam(ps, entry.getKey() + 1, entry.getValue());
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        E entity = mapper.buildEntity();
                        for (int i = 0; i < mapper.columns().size(); i++) {
                            String columnName = mapper.columns().get(i);
                            Object columnValue = getObject(rs, mapper.fields().get(i).getType(), null, columnName);
                            mapper.setters().get(i).run(entity, columnValue);
                        }
                        list.add(entity);
                    }
                }
            }
        } catch (Throwable e) {
            throw handleException(e);
        } finally {
            if (conn != null) {
                connCloser.accept(conn);
            }
        }
        if (saveLog) {
            log.debug(list.size() + " row(s) in " + (System.currentTimeMillis() - startTime) + " ms");
        }
        return list;
    }

    @Override
    public List<LinkedHashMap<String, String>> selectLinkedHashMap(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params) {
        long startTime = System.currentTimeMillis();
        if (saveLog) {
            log.debug(sql);
            log.debug(params.toString());
        }
        List<LinkedHashMap<String, String>> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = connGetter.get();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                    setParam(ps, entry.getKey() + 1, entry.getValue());
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
                        list.add(row);
                    }
                }
            }
        } catch (Exception e) {
            throw handleException(e);
        } finally {
            if (conn != null) {
                connCloser.accept(conn);
            }
        }
        if (saveLog) {
            log.debug(list.size() + " row(s) in " + (System.currentTimeMillis() - startTime) + " ms");
        }
        return list;
    }

    @Override
    public <R> R selectColumn(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params, Class<R> c) {
        long startTime = System.currentTimeMillis();
        if (saveLog) {
            log.debug(sql);
            log.debug(params.toString());
        }
        Connection conn = null;
        try {
            conn = connGetter.get();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                    setParam(ps, entry.getKey() + 1, entry.getValue());
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        if (saveLog) {
                            log.debug("1 row(s) in " + (System.currentTimeMillis() - startTime) + " ms");
                        }
                        return getObject(rs, c, 1, null);
                    }
                }
            }
        } catch (Exception e) {
            throw handleException(e);
        } finally {
            if (conn != null) {
                connCloser.accept(conn);
            }
        }
        return null;
    }

    @Override
    public <R> Object insert(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params, Class<R> c) {
        long startTime = System.currentTimeMillis();
        if (saveLog) {
            log.debug(sql);
            log.debug(params.toString());
        }
        Object primaryKeyValue = null;
        Connection conn = null;
        try {
            conn = connGetter.get();
            try (PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                    setParam(ps, entry.getKey() + 1, entry.getValue());
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
            throw handleException(e);
        } finally {
            if (conn != null) {
                connCloser.accept(conn);
            }
        }
        if (saveLog) {
            log.debug((primaryKeyValue == null ? "0" : "1") + " row(s) in " + (System.currentTimeMillis() - startTime) + " ms");
        }
        return primaryKeyValue;
    }

    @Override
    public long update(Supplier<Connection> connGetter, Consumer<Connection> connCloser, String sql, Map<Integer, Object> params) {
        long startTime = System.currentTimeMillis();
        if (saveLog) {
            log.debug(sql);
            log.debug(params.toString());
        }
        Connection conn = null;
        try {
            conn = connGetter.get();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                    setParam(ps, entry.getKey() + 1, entry.getValue());
                }
                int rowsAffected = ps.executeUpdate();
                if (saveLog) {
                    log.debug(rowsAffected + " row(s) in " + (System.currentTimeMillis() - startTime) + " ms");
                }
                return rowsAffected;
            }
        } catch (Exception e) {
            throw handleException(e);
        } finally {
            if (conn != null) {
                connCloser.accept(conn);
            }
        }
    }

    private static final RuntimeException handleException(Throwable e) {
        if (e instanceof SQLException) {
            return new SqlExecuteException((SQLException) e);
        }
        return new PignooRuntimeException(e);
    }

    @SuppressWarnings("unchecked")
    private static final <R> R getObject(ResultSet rs, Class<R> c, Integer index, String columnLabel) throws SQLException {
        Class<?> getterClass = c;
        if (c.isEnum()) {
            getterClass = String.class;
        } else if (Character.class.isAssignableFrom(c) || char.class.equals(c)) {
            getterClass = String.class;
        } else if (Instant.class.isAssignableFrom(c)) {
            getterClass = Date.class;
        }
        Object value = null;
        if (index != null) {
            value = rs.getObject(index, getterClass);
        } else if (columnLabel != null) {
            value = rs.getObject(columnLabel, getterClass);
        }
        if (value == null) {
            return null;
        }
        R result = null;
        if (c.isEnum()) {
            result = (R) convertEnum(value, c);
        } else if (Character.class.isAssignableFrom(c) || char.class.equals(c)) {
            result = (R) Character.valueOf(value.toString().charAt(0));
        } else if (Instant.class.isAssignableFrom(c)) {
            log.debug("Instant:" + value);
            result = (R) ((Date) value).toInstant();
        } else {
            result = (R) value;
        }
        return result;
    }

    private static final void setParam(PreparedStatement ps, int index, Object value) throws SQLException {
        if (value instanceof Character) {
            ps.setObject(index, value.toString());
        } else if (value.getClass().isEnum()) {
            ps.setObject(index, value.toString());
        } else {
            ps.setObject(index, value);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Enum<T>> T convertEnum(Object dbValue, Class<?> enumType) {
        Class<T> concreteEnumType = (Class<T>) enumType;
        if (dbValue instanceof String) {
            return Enum.valueOf(concreteEnumType, (String) dbValue);
        }
        throw new MapperException("Invalid enum value: " + dbValue + " -> " + enumType);
    }
}
