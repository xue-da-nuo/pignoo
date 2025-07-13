package com.xuesinuo.pignoo.demo.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.Gru;
import com.xuesinuo.pignoo.core.PignooFilter.FMode;
import com.xuesinuo.pignoo.demo.table.Pig;

@SpringBootTest
public class Demo03_Gru {

    @Autowired
    public Gru gru;

    @Test
    public void getById() {
        Long id = 1L;
        var pig = gru.run(pignoo -> {
            var pigList = pignoo.getPignooList(Pig.class);
            return pigList.filter(Pig::getId, FMode.EQ, id).getOne();
        });
        System.out.println("Test01.getById():" + pig);
    }

    @Test
    public void updateNameById() {
        Long id = 1L;
        String name = "Peppa Pig";
        gru.runTransaction(pignoo -> {
            var pigList = pignoo.getPignooList(Pig.class);
            Pig pig = pigList.filter(Pig::getId, FMode.EQ, id).getOne();
            pig.setName(name);
            return null;
        });
    }

    @Test
    public void updateById() {
        Pig pig = new Pig();
        pig.setId(2L);
        pig.setName("Peppa Pig");
        gru.runTransaction(pignoo -> {
            var pigList = pignoo.getPignooList(Pig.class);
            pigList.mixByPk(pig);
            return null;
        });
    }

    @Test
    public void updateMore() {
        gru.runTransaction(pignoo -> {
            var pigList = pignoo.getPignooList(Pig.class);
            var pigs = pigList.filter(Pig::getId, FMode.GT, 2L)
                    .filter(Pig::getName, FMode.LIKE, "戴夫%")
                    .getAll();
            pigs.forEach(pig -> pig.setName("戴夫" + System.currentTimeMillis()));
            return null;
        });
    }

    @Test
    public void deleteAndRowback() {
        try {
            gru.runTransaction(pignoo -> {
                var pigList = pignoo.getPignooList(Pig.class);
                var pigs = pigList.get(2, 2);
                System.out.println("Test01.deleteAndRowback():" + pigs);
                var ids = pigs.stream().map(Pig::getId).toList();
                pigList.copy().filter(Pig::getId, FMode.IN, ids).removeAll();
                pigs = pigList.get(2, 2);
                System.out.println("Test01.deleteAndRowback():" + pigs);
                throw new RuntimeException("Rowback");
            });
        } catch (Exception e) {
            System.out.println("在gru外捕获异常不会影响事务回滚");
        }
    }
}
