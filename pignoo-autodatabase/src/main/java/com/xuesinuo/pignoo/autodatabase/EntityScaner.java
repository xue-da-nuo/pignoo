package com.xuesinuo.pignoo.autodatabase;

import java.util.HashSet;
import java.util.Set;

import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.annotation.Table;
import com.xuesinuo.pignoo.core.entity.EntityMapper;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import lombok.extern.slf4j.Slf4j;

/**
 * 实体扫描器
 * <p>
 * Entity scanner
 * 
 * @author xuesinuo
 * @since 0.3.0
 * @version 0.3.0
 */
@Slf4j
public class EntityScaner {
    /**
     * 扫描指定包下的所有实体类
     * <p>
     * Scan all entity classes under the specified package
     * 
     * @param pignooConfig     pignoo配置
     *                         <p>
     *                         pignoo configuration
     * @param hasChildPackages 是否扫描子包
     *                         <p>
     *                         Whether to scan subpackages
     * @param annClassOnly     是否只扫描带有注解的类
     *                         <p>
     *                         Whether to scan only classes with annotations
     * @param packages         包名
     *                         <p>
     *                         package name
     * @return 实体Mapper集合
     *         <p>
     *         EntityMapper collection
     */
    public static Set<EntityMapper<?>> scan(PignooConfig pignooConfig, boolean hasChildPackages, boolean annClassOnly, String... packages) {
        ClassGraph classGraph = new ClassGraph();
        classGraph.enableAllInfo();
        if (hasChildPackages) {
            classGraph.acceptPackages(packages);
        } else {
            classGraph.acceptPackagesNonRecursive(packages);
        }
        Set<EntityMapper<?>> entityMappers = new HashSet<>();
        if (annClassOnly) {
            classGraph.enableAnnotationInfo();
        }
        try (ScanResult scanResult = classGraph.scan()) {
            ClassInfoList classInfo4GraphList = null;
            if (annClassOnly) {
                classInfo4GraphList = scanResult.getClassesWithAnnotation(Table.class);
            } else {
                classInfo4GraphList = scanResult.getAllClasses();
            }
            classInfo4GraphList.stream()
                    .filter(classInfo -> isPublicClass(classInfo) || isStaticInnerClass(classInfo))// 只识别public静态内部类和public普通类
                    .forEach(classInfo4Graph -> {
                        Class<?> entityClass = classInfo4Graph.loadClass();
                        EntityMapper<?> mapper = null;
                        mapper = EntityMapper.build(entityClass, pignooConfig);
                        entityMappers.add(mapper);
                        log.info("[Pignoo-scan] class: {}, table-name: {}", entityClass.getName(), mapper.tableName());
                    });
        }
        return entityMappers;
    }

    private static boolean isPublicClass(ClassInfo classInfo) {
        return !classInfo.getName().contains("$")
                && classInfo.getModifiersStr().indexOf("public") >= 0;
    }

    private static boolean isStaticInnerClass(ClassInfo classInfo) {
        return classInfo.getName().contains("$")
                && classInfo.getModifiersStr().indexOf("static") >= 0
                && classInfo.getModifiersStr().indexOf("public") >= 0;
    }

}
