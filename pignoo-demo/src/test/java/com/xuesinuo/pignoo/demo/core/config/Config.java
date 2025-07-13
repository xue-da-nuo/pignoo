package com.xuesinuo.pignoo.demo.core.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.xuesinuo.pignoo.core.Gru;
import com.xuesinuo.pignoo.core.Pignoo.DatabaseEngine;

@Configuration
public class Config {
    @Bean
    Gru gru(DataSource dataSource) {
        return new Gru(DatabaseEngine.MySQL, dataSource);
    }
}
