package com.xuesinuo.pignoo.demo.core;

import java.math.BigDecimal;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooFilter;
import com.xuesinuo.pignoo.core.Pignoo.DatabaseEngine;
import com.xuesinuo.pignoo.core.PignooFilter.FMode;
import com.xuesinuo.pignoo.core.PignooSorter.SMode;
import com.xuesinuo.pignoo.core.implement.BasePignoo;
import com.xuesinuo.pignoo.demo.table.Pig;

/**
 * 查询案例
 */
@SpringBootTest
public class Demo02_Query {

    public Pignoo pignoo;

    public Demo02_Query(@Autowired DataSource dataBase) {
        pignoo = new BasePignoo(DatabaseEngine.MySQL, dataBase, false);
    }

    @Test
    public void filterModes() {
        var pigList = pignoo.getPignooList(Pig.class);

        pigList.filter(Pig::getName, FMode.LIKE, "新猪报道%");
        System.out.println(pigList.getOne());// name like '新猪报道%'

        pigList.filter(Pig::getWeight, FMode.GT, new BigDecimal("10"));
        System.out.println(pigList.getOne());// name like '新猪报道%', and then weight > 10
    }

    @Test
    public void multiFilter() {
        var pigList = pignoo.getPignooList(Pig.class);
        pigList.filter(PignooFilter.build(Pig::getWeight, FMode.GT, 10).or(PignooFilter.build(Pig::getWeight, FMode.LT, 2)));
    }
}
