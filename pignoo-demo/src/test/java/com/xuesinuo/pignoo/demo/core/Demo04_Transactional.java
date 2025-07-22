package com.xuesinuo.pignoo.demo.core;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.Gru;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.config.DatabaseEngine;
import com.xuesinuo.pignoo.core.implement.BasePignoo;
import com.xuesinuo.pignoo.core.implement.TransactionPignoo;
import com.xuesinuo.pignoo.demo.table.Pig;

/**
 * 事务：Pignoo是一个“可关闭”的类{@link java.lang.AutoCloseable}，关闭时，会调用{@link java.sql.Connection#close()}
 * <p>
 * 如果使用Pignoo自带的事务管理，建议使用{@link Gru}，否则就需要多写很多事务控制代码！
 * <p>
 * 或者可以用Pignoo-Spring，依赖Spring的事务管理
 */
@SpringBootTest
public class Demo04_Transactional {

    @Autowired
    public DataSource dataSource;

    /**
     * 非事务写法
     */
    @Test
    public void noTransactional() {
        PignooConfig config = new PignooConfig();
        config.setEngine(DatabaseEngine.MySQL);// 可选配置，不填写就会自动识别，增加数据库访问开销
        try (BasePignoo pignoo = new BasePignoo(dataSource, config)) {// jdk7的try-with-resources语法，会自动关闭pignoo
            var pigList = pignoo.writer(Pig.class);
            Pig pig = new Pig();
            pig.setName("新的小猪");
            pig = pigList.add(pig);
            pig.setAge(2);
        }
    }

    /**
     * 原生事务写法
     * <p>
     * 这种不建议使用，替换成{@link Gru}或者Spring更佳
     */
    @Test
    public void nativeTransactional() {
        PignooConfig config = new PignooConfig();
        config.setEngine(DatabaseEngine.MySQL);
        try (TransactionPignoo pignoo = new TransactionPignoo(dataSource, config)) {
            try {
                var pigList = pignoo.writer(Pig.class);
                Pig pig = new Pig();
                pig.setName("新的小猪");
                pig = pigList.add(pig);
                pig.setAge(2);
                // 如果需要，rollback()可以写在任何地方，手动控制
            } catch (Exception e) {
                pignoo.rollback(); // 一般情况，遇到异常回滚，否则退出外层try时自动提交
            }
        } // 关闭pignoo时，会自动提交
    }
}
