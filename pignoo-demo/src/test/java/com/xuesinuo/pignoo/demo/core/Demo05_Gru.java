package com.xuesinuo.pignoo.demo.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.Gru;
import com.xuesinuo.pignoo.demo.table.Pig;

/**
 * Gru是Pignoo原生的事务控制方式，不融合其他框架也能方便控制事务
 */
@SpringBootTest
public class Demo05_Gru {

    @Autowired
    public Gru gru;

    /**
     * 非事务写法
     */
    @Test
    public void noTransactional() {
        Pig newPig = gru.run(pignoo -> {// 这是pignoo的作用域，除了作用域，pignoo不再生效
            var pigList = pignoo.writer(Pig.class);
            Pig pig = new Pig();
            pig.setName("新的小猪");
            pig = pigList.add(pig);
            pig.setAge(2);
            return pig;
        });
        newPig.setAge(3);// 更细数据库无效！pignoo关闭后，操作对象不再更新数据库。直观得说：对象操作只在pignoo作用域内有效
    }

    /**
     * Gru事务写法
     */
    @Test
    public void gruTransactional() {
        gru.runTransaction(pignoo -> {// 事务开始
            var pigList = pignoo.writer(Pig.class);
            Pig pig = new Pig();
            pig.setName("新的小猪");
            pig = pigList.add(pig);
        });// 正常结束自动提交
    }

    /**
     * Gru事务写法，测试回滚
     */
    @Test
    public void rollback() {
        try {// 外层捕获异常不会影响事务回滚，Exception抛到pignoo作用域外就会回滚，平时写代码不用写这层try-catch
            gru.runTransaction(pignoo -> {
                var pigList = pignoo.writer(Pig.class);
                Pig pig = new Pig();
                pig.setName("应该被回滚掉的小猪");
                pigList.add(pig);
                throw new RuntimeException("测试事务回滚");// 测试事务回滚
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
