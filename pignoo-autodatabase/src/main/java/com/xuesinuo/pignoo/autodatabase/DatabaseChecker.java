package com.xuesinuo.pignoo.autodatabase;

import com.xuesinuo.pignoo.autodatabase.entity.DatabaseCheckResult;
import com.xuesinuo.pignoo.core.entity.EntityMapper;

/**
 * 数据库检查器：用于检查数据库是否正确映射了实体类
 * <p>
 * Database Checker: Used to check whether the database correctly maps the entity class
 * 
 * @author xuesinuo
 * @since 0.3.0
 * @version 0.3.0
 */
public interface DatabaseChecker {
    /**
     * 检查数据库是否正确映射了实体类
     * <p>
     * Check whether the database correctly maps the entity class
     * 
     * @param entityMapper 实体映射器
     *                     <p>
     *                     Entity Mapper
     * @return 检查结果
     *         <p>
     *         Check Result
     */
    public DatabaseCheckResult check(EntityMapper<?> entityMapper);
}
