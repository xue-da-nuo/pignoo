package com.xuesinuo.pignoo.demo;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.xuesinuo.pignoo.Pigpen;
import com.xuesinuo.pignoo.Pignoo.DatabaseEngine;

@Configuration
public class PignooConfig {
    @Bean
    Pigpen pigpen(DataSource dataSource) {
        return new Pigpen(DatabaseEngine.MySQL, dataSource);
    }
}
