package com.xuesinuo.pignoo.demo.spring;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.demo.table.Pig;

/**
 * 最简单的上手案例
 */
@SpringBootTest
public class Demo01_EasyStart {

    private final Pignoo pignoo;

    public Demo01_EasyStart(@Autowired Pignoo pignoo) {
        this.pignoo = pignoo;
    }

    @Test
    public void add() {
        var writer = pignoo.writer(Pig.class);
        Pig pig = new Pig();
        pig.setName("新猪报道");
        pig.setPigWeight(new BigDecimal("10.2"));
        pig.setAge(2);
        pig.setColor("白");
        pig = writer.add(pig);
        System.out.println(pig);
    }

    @Test
    public void update() {
        var writer = pignoo.writer(Pig.class);
        Pig pig = writer.filter(Pig::getId, "==", 1).getOne();
        if (pig != null) {
            pig.setName("小猪改名");
        }
    }

    @Test
    public void delete() {
        var writer = pignoo.writer(Pig.class);
        writer.filter(Pig::getId, "==", 10).removeAll();
    }

    @Test
    public void query() {
        var writer = pignoo.writer(Pig.class);
        List<Pig> pigs = writer.filter(Pig::getId, "==", 1).getAll();
        System.out.println(pigs);
    }
}
