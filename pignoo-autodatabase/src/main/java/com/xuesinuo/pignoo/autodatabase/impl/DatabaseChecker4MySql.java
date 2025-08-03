package com.xuesinuo.pignoo.autodatabase.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.sql.DataSource;

import com.xuesinuo.pignoo.autodatabase.DatabaseChecker;
import com.xuesinuo.pignoo.autodatabase.TypeMapper;
import com.xuesinuo.pignoo.autodatabase.entity.DatabaseCheckResult;
import com.xuesinuo.pignoo.core.SqlExecuter;
import com.xuesinuo.pignoo.core.entity.EntityMapper;
import com.xuesinuo.pignoo.core.implement.SimpleJdbcSqlExecuter;

/**
 * MySQL数据库的检查工具
 * <p>
 * Database checker for MySQL
 * 
 * @author xuesinuo
 * @since 0.3.0
 * @version 0.3.0
 */
public class DatabaseChecker4MySql implements DatabaseChecker {
    /**
     * SQL执行器
     * <p>
     * SQL Executer
     */
    protected static final SqlExecuter sqlExecuter = SimpleJdbcSqlExecuter.getInstance();

    private final DataSource dataSource;

    private final TypeMapper typeMapper;

    public DatabaseChecker4MySql(DataSource dataSource, TypeMapper typeMapper) {
        this.dataSource = dataSource;
        this.typeMapper = typeMapper;
    }

    @Override
    public DatabaseCheckResult check(EntityMapper<?> entityMapper) {
        DatabaseCheckResult result = new DatabaseCheckResult();
        Connection conn = null;
        Boolean autoCommit = null;
        try {
            conn = dataSource.getConnection();
            autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(true);
            final Connection c = conn;
            // 是否选中了数据库
            String database = sqlExecuter.selectColumn(() -> c, x -> {}, "SELECT DATABASE()", new HashMap<>(), String.class);
            if (database == null || database.isBlank()) {
                throw new RuntimeException("Connection must have a database selected.");
            }
            // 表是否存在
            String tableName = entityMapper.tableName();
            Integer hasTable = sqlExecuter.selectColumn(() -> c, x -> {},
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='" + database + "' AND TABLE_NAME='" + tableName + "'",
                    new HashMap<>(), Integer.class);
            if (hasTable == null || hasTable == 0) {// 表不存在：创建表
                StringBuilder sql = new StringBuilder();
                String pkColumn = entityMapper.primaryKeyColumn();
                String pkType = this.javaType2SqlType(entityMapper.primaryKeyField().getType());
                if (pkType == null || pkType.isBlank()) {
                    result.getOtherMessage().add("'" + pkColumn + "' in " + entityMapper.getType().getName() + " can't be mapped to a sql type.");
                }
                String pkAuto = entityMapper.autoPrimaryKey() ? "AUTO_INCREMENT" : "";
                sql.append("CREATE TABLE `" + tableName + "` ( ");
                sql.append("`" + pkColumn + "` " + pkType + " NOT NULL " + pkAuto + ", ");
                for (int i = 0; i < entityMapper.columns().size(); i++) {
                    if (entityMapper.columns().get(i).equals(entityMapper.primaryKeyColumn())) {
                        continue;
                    }
                    String column = entityMapper.columns().get(i);
                    String columnType = this.javaType2SqlType(entityMapper.fields().get(i).getType());
                    if (columnType == null || columnType.isBlank()) {
                        result.getOtherMessage().add("'" + column + "' in " + entityMapper.getType().getName() + " can't be mapped to a sql type.");
                    }
                    sql.append("`" + column + "` " + columnType + " DEFAULT NULL, ");
                }
                sql.append("PRIMARY KEY (`" + pkColumn + "`) ");
                sql.append(") ");
                if (!result.getOtherMessage().isEmpty()) {
                    return result;
                }
                result.getAdvise2AddTable().add(sql.toString());
            } else {// 表存在：检查字段
                String sql = """
                        SELECT
                            c.COLUMN_NAME AS `column_name`,
                            c.DATA_TYPE AS `column_type`,
                            CASE
                                WHEN k.COLUMN_NAME IS NOT NULL THEN '1'
                                ELSE '0'
                            END AS `pk`,
                            CASE
                                WHEN c.EXTRA = 'auto_increment' THEN '1'
                                ELSE '0'
                            END AS `auto`
                        FROM
                            information_schema.COLUMNS c
                        LEFT JOIN
                            information_schema.KEY_COLUMN_USAGE k
                            ON c.TABLE_SCHEMA = k.TABLE_SCHEMA
                            AND c.TABLE_NAME = k.TABLE_NAME
                            AND c.COLUMN_NAME = k.COLUMN_NAME
                            AND k.CONSTRAINT_NAME = 'PRIMARY'
                        WHERE
                            c.TABLE_SCHEMA = '__database_name__'
                            AND c.TABLE_NAME = '__table_name__'
                        ORDER BY
                            c.ORDINAL_POSITION;
                        """;
                sql = sql.replaceAll("__database_name__", database).replaceAll("__table_name__", tableName);
                List<LinkedHashMap<String, String>> columnInfosInDatabase = sqlExecuter.selectLinkedHashMap(() -> c, (x) -> {}, sql, new HashMap<>());
                List<String> columnNamesInDatabase = columnInfosInDatabase.stream().map(x -> x.get("column_name")).toList();
                for (int i = 0; i < entityMapper.columns().size(); i++) {// 数据库中缺少字段：添加
                    String column = entityMapper.columns().get(i);
                    String columnType = this.javaType2SqlType(entityMapper.fields().get(i).getType());
                    if (columnType == null || columnType.isBlank()) {
                        result.getOtherMessage().add("'" + column + "' in " + entityMapper.getType().getName() + " can't be mapped to a sql type.");
                        continue;
                    }
                    if (columnNamesInDatabase.contains(column)) {
                        continue;
                    }
                    if (entityMapper.primaryKeyColumn().equals(column)) {
                        result.getOtherMessage().add("Primary-Key '" + column + "' in " + entityMapper.getType().getName() + ", not in table: " + tableName);
                        continue;
                    }
                    sql = "ALTER TABLE `" + tableName + "` ADD COLUMN `" + column + "` " + columnType + " NULL ";
                    result.getAdvise2AddColumn().add(sql);
                }
                for (int i = 0; i < columnInfosInDatabase.size(); i++) {// 数据库中多余字段：删除
                    String column = columnNamesInDatabase.get(i);
                    if (entityMapper.columns().contains(columnNamesInDatabase.get(i))) {
                        continue;
                    }
                    if (columnInfosInDatabase.get(i).get("pk").equals("1")) {
                        result.getOtherMessage().add("Primary-Key '" + column + "' not in " + entityMapper.getType().getName() + ", in table: " + tableName);
                        continue;
                    }
                    sql = "ALTER TABLE `" + tableName + "` DROP COLUMN `" + column + "` ";
                    result.getAdvise2RemoveColumn().add(sql);
                }
                for (int i = 0; i < entityMapper.columns().size(); i++) {// 数据库中字段类型不匹配：修改
                    String column = entityMapper.columns().get(i);
                    if (!columnNamesInDatabase.contains(column)) {
                        continue;
                    }
                    LinkedHashMap<String, String> ciid = columnInfosInDatabase.stream().filter(cid -> cid.get("column_name").equals(column)).findFirst().get();
                    if (column.equals(entityMapper.primaryKeyColumn()) && ciid.get("pk").equals("1") && (entityMapper.autoPrimaryKey() != ciid.get("auto").equals("1"))) {
                        result.getOtherMessage().add("Primary-Key auto setting is error in " + entityMapper.getType().getName() + " and table: " + tableName);
                    }
                    if (this.javaType2SqlTypeOk(entityMapper.fields().get(i).getType(), ciid.get("column_type"))) {
                        continue;
                    }
                    if (entityMapper.primaryKeyColumn().equals(column) || ciid.get("pk").equals("1")) {
                        result.getOtherMessage().add("Primary-Key type mapping error with " + entityMapper.getType().getName() + " and table: " + tableName);
                        continue;
                    }
                    String columnType = this.javaType2SqlType(entityMapper.fields().get(i).getType());
                    if (columnType == null || columnType.isBlank()) {
                        result.getOtherMessage().add("'" + column + "' in " + entityMapper.getType().getName() + " can't be mapped to a sql type.");
                        continue;
                    }
                    sql = "ALTER TABLE `" + tableName + "` MODIFY COLUMN `" + column + "` " + columnType + " ";
                    result.getAdvise2UpdateColumn().add(sql);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    if (autoCommit != null) {
                        conn.setAutoCommit(autoCommit);
                    }
                    conn.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return result;
    }

    /**
     * Java类型转为MySQL类型
     * <p>
     * Java type is converted to MySQL type
     * 
     * @param javaType Java类型
     *                 <p>
     *                 Java type
     * @return MySQL类型
     *         <p>
     *         MySQL type
     */
    private String javaType2SqlType(Class<?> javaType) {
        String sqlType = typeMapper.javaTypeToSqlType(javaType);
        if (sqlType == null || sqlType.isBlank()) {
            sqlType = defaultTypeMapper.javaTypeToSqlType(javaType);
        }
        return sqlType;
    }

    /**
     * Java类型转为MySQL类型的默认转换方式
     * <p>
     * Java type is converted to MySQL type by default
     */
    public static final TypeMapper defaultTypeMapper = javaType -> {
        // 基本数据类型
        if (Long.class.isAssignableFrom(javaType) || long.class.equals(javaType)) {
            return "bigint";
        }
        if (Integer.class.isAssignableFrom(javaType) || int.class.equals(javaType)) {
            return "int";
        }
        if (Short.class.isAssignableFrom(javaType) || short.class.equals(javaType)) {
            return "smallint";
        }
        if (Byte.class.isAssignableFrom(javaType) || byte.class.equals(javaType)) {
            return "tinyint";
        }
        if (Double.class.isAssignableFrom(javaType) || double.class.equals(javaType)) {
            return "double";
        }
        if (Float.class.isAssignableFrom(javaType) || float.class.equals(javaType)) {
            return "float";
        }
        if (Boolean.class.isAssignableFrom(javaType) || boolean.class.equals(javaType)) {
            return "tinyint(1)";
        }
        if (Character.class.isAssignableFrom(javaType) || char.class.equals(javaType)) {
            return "char(1)";
        }
        // 字符串
        if (String.class.isAssignableFrom(javaType)) {
            return "varchar(255)";
        }
        // 日期时间
        if (java.util.Date.class.isAssignableFrom(javaType)) {
            if (java.sql.Timestamp.class.isAssignableFrom(javaType)) {
                return "datetime";
            }
            return "date";
        }
        if (Instant.class.isAssignableFrom(javaType)) {
            return "timestamp";
        }
        if (LocalDate.class.isAssignableFrom(javaType)) {
            return "date";
        }
        if (LocalTime.class.isAssignableFrom(javaType)) {
            return "time";
        }
        if (LocalDateTime.class.isAssignableFrom(javaType)) {
            return "datetime";
        }
        if (ZonedDateTime.class.isAssignableFrom(javaType) || OffsetDateTime.class.isAssignableFrom(javaType)) {
            return "timestamp";
        }
        if (OffsetTime.class.isAssignableFrom(javaType)) {
            return "time";
        }
        // 数字
        if (java.math.BigDecimal.class.isAssignableFrom(javaType)) {
            return "decimal(32,4)";
        }
        if (BigInteger.class.isAssignableFrom(javaType)) {
            return "decimal(64,0)";
        }
        // 二进制
        if (byte[].class.isAssignableFrom(javaType)) {
            return "blob";
        }
        // 枚举
        if (javaType.isEnum()) {
            return "varchar(255)";
        }
        return null;
    };

    private static final List<String> LONG_TYPES = List.of("bigint");
    private static final List<String> INTEGER_TYPES = List.of("int", "integer", "mediumint");
    private static final List<String> SHORT_TYPES = List.of("smallint");
    private static final List<String> BYTE_TYPES = List.of("tinyint");
    private static final List<String> DOUBLE_TYPES = List.of("double", "float", "real");
    private static final List<String> FLOAT_TYPES = List.of("float", "real");
    private static final List<String> BOOLEAN_TYPES = List.of("tinyint(1)", "bit(1)", "boolean");
    private static final List<String> CHARACTER_TYPES = List.of("char(1)", "varchar(1)");
    private static final List<String> STRING_TYPES = List.of("char", "varchar", "text", "tinytext", "mediumtext", "longtext", "json");
    private static final List<String> BIG_INTEGER_TYPES = List.of("bigint", "decimal", "numeric");
    private static final List<String> BIG_DECIMAL_TYPES = List.of("decimal", "numeric");
    private static final List<String> UTIL_DATE_TYPES = List.of("date", "datetime", "timestamp", "time");
    private static final List<String> LOCAL_DATE_TYPES = List.of("date");
    private static final List<String> LOCAL_TIME_TYPES = List.of("time");
    private static final List<String> LOCAL_DATE_TIME_TYPES = List.of("datetime", "timestamp");
    private static final List<String> INSTANT_TYPES = List.of("datetime", "timestamp");
    private static final List<String> ZONED_OR_OFFSET_DATE_TIME_TYPES = List.of("datetime", "timestamp");
    private static final List<String> OFFSET_TIME_TYPES = List.of("time");
    private static final List<String> BINARY_TYPES = List.of("blob", "tinyblob", "mediumblob", "longblob", "binary", "varbinary");
    private static final List<String> ENUM_TYPES = List.of("enum", "char", "varchar");

    /**
     * 验证Java类型与SQL类型是否可以映射
     * <p>
     * Verify whether Java type and SQL type can be mapped
     * 
     * @param javaType Java类型
     *                 <p>
     *                 Java type
     * @param sqlType  SQL类型
     *                 <p>
     *                 SQL type
     * @return 能否映射
     *         <p>
     *         Can it be mapped
     */
    private boolean javaType2SqlTypeOk(Class<?> javaType, String mysqlType) {
        // 基本数据类型
        if (Long.class.isAssignableFrom(javaType) || long.class.equals(javaType))
            return LONG_TYPES.contains(mysqlType);
        if (Integer.class.isAssignableFrom(javaType) || int.class.equals(javaType))
            return INTEGER_TYPES.contains(mysqlType);
        if (Short.class.isAssignableFrom(javaType) || short.class.equals(javaType))
            return SHORT_TYPES.contains(mysqlType);
        if (Byte.class.isAssignableFrom(javaType) || byte.class.equals(javaType))
            return BYTE_TYPES.contains(mysqlType);
        if (Double.class.isAssignableFrom(javaType) || double.class.equals(javaType))
            return DOUBLE_TYPES.contains(mysqlType);
        if (Float.class.isAssignableFrom(javaType) || float.class.equals(javaType))
            return FLOAT_TYPES.contains(mysqlType);
        if (Boolean.class.isAssignableFrom(javaType) || boolean.class.equals(javaType))
            return BOOLEAN_TYPES.contains(mysqlType);
        if (Character.class.isAssignableFrom(javaType) || char.class.equals(javaType))
            return CHARACTER_TYPES.contains(mysqlType);

        // String
        if (String.class.isAssignableFrom(javaType))
            return STRING_TYPES.contains(mysqlType);

        // 数字
        if (BigInteger.class.isAssignableFrom(javaType))
            return BIG_INTEGER_TYPES.contains(mysqlType);
        if (BigDecimal.class.isAssignableFrom(javaType))
            return BIG_DECIMAL_TYPES.contains(mysqlType);

        // 日期时间
        if (java.util.Date.class.isAssignableFrom(javaType))
            return UTIL_DATE_TYPES.contains(mysqlType);
        if (LocalDate.class.isAssignableFrom(javaType))
            return LOCAL_DATE_TYPES.contains(mysqlType);
        if (LocalTime.class.isAssignableFrom(javaType))
            return LOCAL_TIME_TYPES.contains(mysqlType);
        if (LocalDateTime.class.isAssignableFrom(javaType))
            return LOCAL_DATE_TIME_TYPES.contains(mysqlType);
        if (Instant.class.isAssignableFrom(javaType))
            return INSTANT_TYPES.contains(mysqlType);
        if (ZonedDateTime.class.isAssignableFrom(javaType) || OffsetDateTime.class.isAssignableFrom(javaType))
            return ZONED_OR_OFFSET_DATE_TIME_TYPES.contains(mysqlType);
        if (OffsetTime.class.isAssignableFrom(javaType))
            return OFFSET_TIME_TYPES.contains(mysqlType);

        // 二进制
        if (byte[].class.isAssignableFrom(javaType))
            return BINARY_TYPES.contains(mysqlType);

        // 枚举
        if (javaType.isEnum())
            return ENUM_TYPES.contains(mysqlType);

        return false;// 未匹配的类型默认返回false
    }
}
