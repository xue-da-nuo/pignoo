package com.xuesinuo.pignoo.demo.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.Gru;
import com.xuesinuo.pignoo.core.PignooSorter.SMode;
import com.xuesinuo.pignoo.core.annotation.Ignore;
import com.xuesinuo.pignoo.core.annotation.Link;
import com.xuesinuo.pignoo.demo.table.Pig;

import lombok.Data;

/**
 * Link用法可以在不考虑SQL要素的情况下，做表的部分列映射，适合用在包含blob之类大字段的表
 */
@SpringBootTest
public class Demo06_Link {
    @Autowired
    public Gru gru;

    @Link(Pig.class)
    @Data
    public static class PigLinker {
        private Long id;
        private String name;
        @Ignore
        private String nickName;
    }

    /**
     * Link写法，忽略掉其他字段。注意：忽略的字段如果是非空且数据库没有设置默认值，则新增会报错的！
     */
    @Test
    public void noTransactional() {
        gru.run(pignoo -> {
            var writer = pignoo.writer(PigLinker.class);
            PigLinker pig = new PigLinker();
            pig.setName("林可儿");
            writer.add(pig);

            System.out.println(writer.sort(PigLinker::getId, SMode.MAX_FIRST).get(0, 10));
        });
    }
}
