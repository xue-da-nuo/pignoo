package com.xuesinuo.pignoo.demo;

import org.springframework.stereotype.Component;

import com.xuesinuo.pignoo.core.Gru;
import com.xuesinuo.pignoo.core.PignooFilter.FMode;
import com.xuesinuo.pignoo.core.annotation.Column;
import com.xuesinuo.pignoo.core.annotation.PrimaryKey;
import com.xuesinuo.pignoo.core.annotation.Table;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class Test01 {

    private final Gru gru;

    @Table("pig")
    @Data
    public static class Pig {
        @Column("id")
        @PrimaryKey(auto = true)
        private Long id;

        @Column("name")
        private String name;
    }

    @PostConstruct
    public void init() {
        this.getById(1L);

        this.updateNameById(1L, "Peppa Pig");

        Pig pig = new Pig();
        pig.setId(2L);
        pig.setName("Peppa Pig");
        this.updateById(pig);

        this.updateMore();

        try {
            this.deleteAndRowback();
        } catch (Exception e) {}
    }

    private void getById(Long id) {
        var pig = gru.run(pignoo -> {
            var pigList = pignoo.getPignooList(Pig.class);
            return pigList.filter(Pig::getId, FMode.EQ, id).getOne();
        });
        System.out.println("Test01.getById():" + pig);
    }

    private void updateNameById(Long id, String name) {
        gru.runTransaction(pignoo -> {
            var pigList = pignoo.getPignooList(Pig.class);
            Pig pig = pigList.filter(Pig::getId, FMode.EQ, id).getOne();
            pig.setName(name);
            return null;
        });
    }

    private void updateById(Pig pig) {
        gru.runTransaction(pignoo -> {
            var pigList = pignoo.getPignooList(Pig.class);
            pigList.mixByPk(pig);
            return null;
        });
    }

    private void updateMore() {
        gru.runTransaction(pignoo -> {
            var pigList = pignoo.getPignooList(Pig.class);
            var pigs = pigList.filter(Pig::getId, FMode.GT, 2L)
                    .filter(Pig::getName, FMode.LIKE, "戴夫%")
                    .getAll();
            pigs.forEach(pig -> pig.setName("戴夫" + System.currentTimeMillis()));
            return null;
        });
    }

    private void deleteAndRowback() {
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
    }
}
