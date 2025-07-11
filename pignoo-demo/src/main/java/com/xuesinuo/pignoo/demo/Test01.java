package com.xuesinuo.pignoo.demo;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;

import com.xuesinuo.pignoo.Pignoo;
import com.xuesinuo.pignoo.PignooList;
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

    private final Pignoo pignoo;

    @Table("pig")
    @Data
    public static class Pig {
        @Column("id")
        @PrimaryKey(auto = true)
        private Long id;
    }

    @PostConstruct
    public void init() {
        PignooList<Pig> pigList = pignoo.getPignooList(Pig.class);
        pigList.add(new Pig());
        log.info(pigList.getAll().toString());
    }
}
