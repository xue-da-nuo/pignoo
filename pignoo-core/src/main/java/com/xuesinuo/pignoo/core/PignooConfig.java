package com.xuesinuo.pignoo.core;

import com.xuesinuo.pignoo.core.config.AnnotationMode;
import com.xuesinuo.pignoo.core.config.AnnotationMode.AnnotationMixMode;
import com.xuesinuo.pignoo.core.config.DatabaseEngine;
import com.xuesinuo.pignoo.core.config.PrimaryKeyNamingConvention;

import lombok.Data;

/**
 * Pignoo的配置
 * <p>
 * Pignoo's configuration
 *
 * @author xuesinuo
 * @since 0.1.0
 * @version 0.2.1
 */
@Data
public class PignooConfig {

    /**
     * 复制一份配置文件
     * <p>
     * Copy a configuration file
     *
     * @version 0.2.1
     * @return 复制后的配置
     *         <p>
     *         copied configuration
     */
    public PignooConfig copy() {
        PignooConfig config = new PignooConfig();
        config.setEngine(this.engine);
        config.setAnnotationMode(this.annotationMode);
        config.setAnnotationMixMode(this.annotationMixMode);
        config.setPrimaryKeyNamingConvention(this.primaryKeyNamingConvention);
        config.setAutoPrimaryKey(this.autoPrimaryKey);
        return config;
    }

    /**
     * 数据库引擎。如果不配置，则尝试从数据库读取
     * <p>
     * Database engine。 If not configured, try to read from the database
     */
    private DatabaseEngine engine;

    /**
     * 注解使用方式
     * <p>
     * Annotation usage
     * 
     * @version 0.2.0
     */
    private AnnotationMode annotationMode = AnnotationMode.MIX;

    /**
     * 注解混合使用方式（当annotationMode=MIX时生效）
     * <p>
     * Annotation mixed usage(when annotationMode=MIX is effective)
     * 
     * @version 0.2.0
     */
    private AnnotationMixMode annotationMixMode = AnnotationMixMode.CAMEL_TO_SNAKE;

    /**
     * 主键命名规则{@link FunctionalInterface}
     * <p>
     * PrimaryKey naming convention{@link FunctionalInterface}
     * 
     * @version 0.2.0
     */
    private PrimaryKeyNamingConvention primaryKeyNamingConvention = PrimaryKeyNamingConvention.DEFAULT;

    /**
     * 是否自动生成主键
     * <p>
     * Whether to automatically generate primary keys
     * 
     * @version 0.2.0
     */
    private Boolean autoPrimaryKey = true;
}
