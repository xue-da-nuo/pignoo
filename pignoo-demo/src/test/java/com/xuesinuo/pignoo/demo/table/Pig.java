package com.xuesinuo.pignoo.demo.table;

import java.math.BigDecimal;

import com.xuesinuo.pignoo.core.annotation.Column;
import com.xuesinuo.pignoo.core.annotation.PrimaryKey;
import com.xuesinuo.pignoo.core.annotation.Table;

import lombok.Data;

@Table("pig")
@Data
public class Pig {
    @Column("id")
    @PrimaryKey(auto = true)
    private Long id;

    @Column("name")
    private String name;

    @Column("age")
    private Integer age;

    @Column("color")
    private String color;

    @Column("weight")
    private BigDecimal weight;
}
