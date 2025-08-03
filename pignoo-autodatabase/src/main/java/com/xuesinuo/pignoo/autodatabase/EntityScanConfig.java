package com.xuesinuo.pignoo.autodatabase;

import com.xuesinuo.pignoo.core.annotation.Table;

import lombok.Data;

/**
 * 扫描器配置
 * <p>
 * Scanner configuration
 * 
 * @author xuesinuo
 * @since 0.3.0
 * @version 0.3.0
 */
@Data
public class EntityScanConfig {

    /**
     * 复制一份配置文件
     * <p>
     * Copy a configuration file
     * 
     * @return 复制后的配置
     *         <p>
     *         copied configuration
     */
    public EntityScanConfig copy() {
        EntityScanConfig entityScanConfig = new EntityScanConfig();
        entityScanConfig.setScanChildPackages(this.scanChildPackages);
        entityScanConfig.setAnnotationClassOnly(this.annotationClassOnly);
        entityScanConfig.setPackages(this.packages);
        entityScanConfig.setClassesForScanPackage(this.classesForScanPackage);
        entityScanConfig.setBuildMode(this.buildMode);
        entityScanConfig.setBreakRunning(this.breakRunning);
        return entityScanConfig;
    }

    /**
     * 自动构建模式
     * <p>
     * Automatic build mode
     */
    public static enum BuildMode {
        /**
         * 谨慎模式：不做任何操作，只给出提示
         * <p>
         * Careful mode: do nothing, just give a hint
         */
        CAREFULLY,

        /**
         * 安全模式：自动执行新增操作，即使自动操作与预期有偏差，也不会影响原有数据
         * <p>
         * Safe mode: automatically execute the add operation, even if the automatic operation deviates from the expected results, it will not affect the original data
         */
        SAFELY,

        /**
         * 易用模式：自动执行新增和修改操作
         * <p>
         * Usability mode: automatically execute add and modify operations
         */
        USABILITY,

        /**
         * 激进模式：尽可能执行任何操作，包括删除
         * <p>
         * Radical mode: try to execute any operation, including deletion
         */
        RADICALLY
    }

    /**
     * 递归扫描子包
     * <p>
     * Recursively scan subpackages
     */
    private Boolean scanChildPackages = true;

    /**
     * 只扫描带有{@link Table}注解的类
     * <p>
     * Only scan classes with {@link Table} annotations
     */
    private Boolean annotationClassOnly = true;

    /**
     * 要扫描的包，与classesForScanPackage同时生效，合并起效
     * <p>
     * Package to scan, effective at the same time as 'classesForScanPackage', merged into effect
     */
    private String[] packages;

    /**
     * 扫描这些类所在包，与packages同时生效，合并起效
     * <p>
     * Scan these classes in the package, effective at the same time as 'packages', merged into effect
     */
    private Class<?>[] classesForScanPackage;

    /**
     * 构建模式，默认为谨慎模式。需要显式得制定你想要的模式，否则工具会做任何数据操作
     * <p>
     * Build mode, default is careful mode. You need to explicitly specify the mode you want, otherwise the tool will do any data operation
     */
    private BuildMode buildMode = BuildMode.CAREFULLY;

    /**
     * 是否打断运行：再遇到无法容忍的问题时，抛出异常
     * <p>
     * Whether to break the run: throw an exception when encountering an unacceptable problem
     */
    private Boolean breakRunning = true;

    /**
     * 自定义类型映射器，将Java类型转为SQL类型。自定义映射无法映射到时，使用默认映射。
     * <p>
     * Custom type mapper, convert Java type to SQL type. When the custom mapping cannot be mapped, use the default mapping.
     */
    private TypeMapper typeMapper = javaType -> null;
}
