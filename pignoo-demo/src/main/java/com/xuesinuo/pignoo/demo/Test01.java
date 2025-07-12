package com.xuesinuo.pignoo.demo;

import org.springframework.stereotype.Component;

import com.xuesinuo.pignoo.PignooList;
import com.xuesinuo.pignoo.Gru;
import com.xuesinuo.pignoo.PignooFilter.FMode;
import com.xuesinuo.pignoo.annotation.Column;
import com.xuesinuo.pignoo.annotation.PrimaryKey;
import com.xuesinuo.pignoo.annotation.Table;

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
        gru.runTransaction(pignoo -> {
            PignooList<Pig> pigList = pignoo.getPignooList(Pig.class);
            Pig newPig = new Pig();
            newPig.setName("戴夫");
            pigList.add(newPig);
            pigList.filter(Pig::getName, FMode.IS_NULL).getOne().setName("艾文");
            log.info(pigList.getAll().toString());
            return null;
        });
    }
}
