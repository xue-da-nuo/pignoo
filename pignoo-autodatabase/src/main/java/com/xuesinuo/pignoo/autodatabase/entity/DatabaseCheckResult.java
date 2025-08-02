package com.xuesinuo.pignoo.autodatabase.entity;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 数据库检查结果
 * <p>
 * Database check result
 * 
 * @author xuesinuo
 * @since 0.3.0
 * @version 0.3.0
 */
@Data
public class DatabaseCheckResult {
    /**
     * 如果表不存在，这里存放创建表的建议语句
     * <p>
     * If the table does not exist, here is the suggestion statement for creating the table
     */
    public List<String> advise2AddTable = new ArrayList<>();

    /**
     * 表中缺少字段时，建议添加字段的语句
     * <p>
     * Here is the suggestion statement for adding fields when there are missing fields in the table
     */
    public List<String> advise2AddColumn = new ArrayList<>();

    /**
     * 表中字段类型不匹配时，建议修改字段的语句
     * <p>
     * Here is the suggestion statement for modifying fields when the field types do not match in the table
     */
    public List<String> advise2UpdateColumn = new ArrayList<>();

    /**
     * 表中多余字段时，建议删除字段的语句
     * <p>
     * Here is the suggestion statement for deleting fields when there are extra fields in the table
     */
    public List<String> advise2RemoveColumn = new ArrayList<>();

    /**
     * 遇到无法通过自动化SQL修正的问题，这里存放问题描述
     * <p>
     * If you encounter a problem that cannot be corrected through automated SQL, here is the problem description
     */
    public List<String> otherMessage = new ArrayList<>();
}
