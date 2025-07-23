package com.xuesinuo.pignoo.core.entity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SQL参数拼接工具
 * 
 * @author xuesinuo
 * @since 0.2.3
 */
public class SqlParam {
    public int index = 0;
    public Map<Integer, Object> params = new LinkedHashMap<>();

    public String next(Object value) {
        params.put(index++, value);
        return "?";
    }
}
