package com.xuesinuo.pignoo.autodatabase;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import com.xuesinuo.pignoo.autodatabase.EntityScanConfig.BuildMode;
import com.xuesinuo.pignoo.autodatabase.entity.DatabaseCheckResult;
import com.xuesinuo.pignoo.autodatabase.impl.DatabaseChecker4MySql;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.annotation.Table;
import com.xuesinuo.pignoo.core.config.DatabaseEngine;
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

    private final PignooConfig pignooConfig;

    private final EntityScanConfig entityScanConfig;

    private final DatabaseChecker databaseChecker;

    public EntityScaner(DataSource dataSource, PignooConfig pignooConfig, EntityScanConfig entityScanConfig) {
        this.pignooConfig = pignooConfig.copy();
        this.entityScanConfig = entityScanConfig.copy();
        if (pignooConfig.getEngine() == null) {
            try (Connection conn = dataSource.getConnection()) {
                pignooConfig.setEngine(DatabaseEngine.getDatabaseEngineByConnection(conn));
                if (!conn.getAutoCommit()) {
                    conn.commit();
                }
            } catch (SQLException e) {
                throw new RuntimeException("Search database engine error", e);
            }
        }
        switch (pignooConfig.getEngine()) {
        case DatabaseEngine.MySQL:
            this.databaseChecker = new DatabaseChecker4MySql(dataSource);
            break;
        }
        throw new RuntimeException("Unknow database engine");
    }

    /**
     * 扫描指定包下的所有实体类
     * <p>
     * Scan all entity classes under the specified package
     * 
     * @return 实体Mapper集合
     *         <p>
     *         EntityMapper collection
     */
    public Set<EntityMapper<?>> scan() {
        HashSet<String> packages = new HashSet<>(entityScanConfig.getPackages().length + entityScanConfig.getClassesForScanPackage().length);
        for (String packageStr : entityScanConfig.getPackages()) {
            if (packageStr != null && !packageStr.isBlank()) {
                packages.add(packageStr);
            }
        }
        for (Class<?> classForScanPackage : entityScanConfig.getClassesForScanPackage()) {
            if (classForScanPackage != null) {
                packages.add(classForScanPackage.getPackage().getName());
            }
        }
        if (packages.isEmpty()) {
            throw new RuntimeException("EntityScanConfig packages is empty");
        }
        String[] packageArray = packages.toArray(new String[packages.size()]);
        ClassGraph classGraph = new ClassGraph();
        classGraph.enableAllInfo();
        if (entityScanConfig.getScanChildPackages()) {
            classGraph.acceptPackages(packageArray);
        } else {
            classGraph.acceptPackagesNonRecursive(packageArray);
        }
        Set<EntityMapper<?>> entityMappers = new HashSet<>();
        if (entityScanConfig.getAnnotationClassOnly()) {
            classGraph.enableAnnotationInfo();
        }
        try (ScanResult scanResult = classGraph.scan()) {
            ClassInfoList classInfo4GraphList = null;
            if (entityScanConfig.getAnnotationClassOnly()) {
                classInfo4GraphList = scanResult.getClassesWithAnnotation(Table.class);
            } else {
                classInfo4GraphList = scanResult.getAllClasses();
            }
            classInfo4GraphList.stream()
                    .filter(classInfo -> isPublicClass(classInfo) || isStaticInnerClass(classInfo))// 只识别public静态内部类和public普通类
                    .forEach(classInfo4Graph -> {
                        Class<?> entityClass = classInfo4Graph.loadClass();
                        EntityMapper<?> mapper = null;
                        mapper = EntityMapper.build(entityClass, this.pignooConfig);
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

    /**
     * 扫描并构建
     * <p>
     * Scan and build
     */
    public void scanAndBuild() {
        DatabaseCheckResult allResult = new DatabaseCheckResult();
        for (EntityMapper<?> mapper : scan()) {
            DatabaseCheckResult itemResult = databaseChecker.check(mapper);
            allResult.getAdvise2AddTable().addAll(itemResult.getAdvise2AddTable());
            allResult.getAdvise2AddColumn().addAll(itemResult.getAdvise2AddColumn());
            allResult.getAdvise2UpdateColumn().addAll(itemResult.getAdvise2UpdateColumn());
            allResult.getAdvise2RemoveColumn().addAll(itemResult.getAdvise2RemoveColumn());
            allResult.getOtherMessage().addAll(itemResult.getOtherMessage());
        }
        String warningTitle = """
                ================================
                = Pingnoo-Autodatabase Warning =
                ================================
                """;
        String errorTitle = """
                ==============================
                = Pingnoo-Autodatabase Error =
                ==============================
                """;
        List<String> workingList = new ArrayList<>();
        StringBuilder warning = new StringBuilder(warningTitle);
        StringBuilder error = new StringBuilder(errorTitle);
        allResult.getOtherMessage().stream().forEach(msg -> error.append(msg).append("\n"));
        // TODO
    }
}
