package com.xuesinuo.pignoo.demo;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.xuesinuo.pignoo.Pignoo;
import com.xuesinuo.pignoo.Pignoo.DatabaseEngine;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class PignooConfig {

    private final DataSource dataSource;

    @Bean
    Pignoo pignoo() {
        Pignoo pignoo = new Pignoo(DatabaseEngine.MySQL, dataSource);
        return pignoo;
    }
}
