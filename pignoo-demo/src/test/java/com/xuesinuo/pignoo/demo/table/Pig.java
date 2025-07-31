package com.xuesinuo.pignoo.demo.table;

import java.math.BigDecimal;

import com.xuesinuo.pignoo.core.annotation.Ignore;

import lombok.Data;

@Data
public class Pig {
    private Long id;

    private String name;

    @Ignore
    private String nickName;

    private Integer age;

    private String color;

    private BigDecimal pigWeight;
}
