package com.xuesinuo.pignoo.demo;

import org.springframework.stereotype.Component;

import com.xuesinuo.pignoo.Pignoo;
import com.xuesinuo.pignoo.PignooList;
import com.xuesinuo.pignoo.Pigpen;
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

    private final Pigpen pigpen;

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
        try (Pignoo pignoo = pigpen.build()) {
            PignooList<Pig> pigList = pignoo.getPignooList(Pig.class);
            pigList.filter(Pig::getName, FMode.IS_NULL).getOne().setName("艾文");
            log.info(pigList.getAll().toString());
        }
    }
}
