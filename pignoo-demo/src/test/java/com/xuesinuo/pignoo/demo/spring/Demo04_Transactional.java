package com.xuesinuo.pignoo.demo.spring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.demo.table.Pig;

/**
 * 事务：SpringPignoo是一个Pignoo的代理对象，它的整个生命周期和Spring容器一致，代码中无需手动调用close方法。
 */
@SpringBootTest
public class Demo04_Transactional {

    private final Pignoo pignoo;
    private final Demo04_Transactional_Tool tool;

    public Demo04_Transactional(@Autowired Pignoo pignoo, @Autowired Demo04_Transactional_Tool tool) {
        this.pignoo = pignoo;
        this.tool = tool;
    }

    /**
     * 非事务写法
     */
    @Test
    public void noTransactional() {
        var pigList = pignoo.getList(Pig.class);
        Pig pig = new Pig();
        pig.setName("新的小猪");
        pig = pigList.add(pig);
        pig.setAge(2);
    }

    /**
     * 事务写法
     */
    @Test
    @Transactional
    public void gruTransactional() {
        var pigList = pignoo.getList(Pig.class);
        Pig pig = new Pig();
        pig.setName("新的小猪");
        pig = pigList.add(pig);
    }

    /**
     * 事务，测试回滚
     */
    @Test
    public void rollback() {
        try {
            tool.rollback();// 这句被@Transactional回滚
        } catch (Exception e) {
            e.printStackTrace();// @Transactional外部捕获异常不影响回滚
        }
    }
}

@Component
class Demo04_Transactional_Tool {
    private final Pignoo pignoo;

    public Demo04_Transactional_Tool(@Autowired Pignoo pignoo) {
        this.pignoo = pignoo;
    }

    @Transactional
    public void rollback() {
        var pigList = pignoo.getList(Pig.class);
        Pig pig = new Pig();
        pig.setName("应该被回滚掉的小猪");
        pigList.add(pig);
        throw new RuntimeException("测试事务回滚");// 测试事务回滚
    }
}
