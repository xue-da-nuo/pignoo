package com.xuesinuo.pignoo.demo.spring.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.spring.implement.SpringPignoo;

@Configuration
public class SpringPignooConfig {
    @Bean
    Pignoo pignoo(@Autowired DataSource dataSource) {
        return new SpringPignoo(dataSource);
    }
}
