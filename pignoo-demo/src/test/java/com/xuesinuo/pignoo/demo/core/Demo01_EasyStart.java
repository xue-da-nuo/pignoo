package com.xuesinuo.pignoo.demo.core;

import java.math.BigDecimal;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.Pignoo.DatabaseEngine;
import com.xuesinuo.pignoo.core.PignooFilter.FMode;
import com.xuesinuo.pignoo.core.PignooSorter.SMode;
import com.xuesinuo.pignoo.core.implement.BasePignoo;
import com.xuesinuo.pignoo.demo.table.Pig;

/**
 * 最简单的上手案例
 */
@SpringBootTest
public class Demo01_EasyStart {

    public Pignoo pignoo;

    public Demo01_EasyStart(@Autowired DataSource dataBase) {
        pignoo = new BasePignoo(DatabaseEngine.MySQL, dataBase, false);
    }

    @Test
    public void add() {
        var pigList = pignoo.getPignooList(Pig.class);
        Pig pig = new Pig();
        pig.setName("新猪报道" + System.currentTimeMillis());
        pig.setWeight(new BigDecimal("10.2"));
        pig.setAge(2);
        pig.setColor("白");
        pig = pigList.add(pig);
        System.out.println(pig);
    }

    @Test
    public void update() {
        var pigList = pignoo.getPignooList(Pig.class);
        Pig pig = pigList.filter(Pig::getId, FMode.EQ, 1).getOne();
        if (pig != null) {
            pig.setName("猪猪改名" + System.currentTimeMillis());
        }
    }

    @Test
    public void delete() {
        var pigList = pignoo.getPignooList(Pig.class);
        Pig pig = pigList.filter(Pig::getId, FMode.NOT_EQ, 1).sort(Pig::getId, SMode.MAX_FIRST).getOne();
        pigList.removeByPk(pig);
    }

    @Test
    public void query() {
        var pigList = pignoo.getPignooList(Pig.class);
        List<Pig> pigs = pigList.filter(Pig::getName, FMode.LIKE, "新猪报道%").get(0, 10);
        System.out.println(pigs);
    }
}
