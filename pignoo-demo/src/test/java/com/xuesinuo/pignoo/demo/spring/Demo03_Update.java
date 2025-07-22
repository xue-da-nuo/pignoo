package com.xuesinuo.pignoo.demo.spring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooSorter.SMode;
import com.xuesinuo.pignoo.demo.table.Pig;

/**
 * 更新数据：PignooWriter中的数据可以1:1理解为数据库中的数据，只操作PignooWriter就能达到操作数据库的效果
 */
@SpringBootTest
public class Demo03_Update {

    private final Pignoo pignoo;

    public Demo03_Update(@Autowired Pignoo pignoo) {
        this.pignoo = pignoo;
    }

    /**
     * 新增数据
     */
    @Test
    public void add() {
        var pigList = pignoo.writer(Pig.class);
        Pig pig = new Pig();
        pig.setName("新的小猪");
        Pig newPig = pigList.add(pig);// 这一步，将pig放入List，等同于将数据插入数据库

        pig.setAge(1);// pig是你new的小猪，不是数据库中的小猪，此时数据库中age=null
        System.out.println(pig);
        newPig.setAge(2);// newPig是PignooWriter中取出的小猪，这一步等同于更新数据库，此时age=2
        System.out.println(newPig);// 有自增ID，有数据库默认属性
    }

    /**
     * 通过查询修改数据
     */
    @Test
    public void updateByQuery() {
        Long id = 1L;
        String name = "猪猪改名字";

        var pigList = pignoo.writer(Pig.class);
        Pig pig = pigList.filter(Pig::getId, "==", id).getOne();
        if (pig != null) {
            pig.setName(name);
        }
    }

    /**
     * 通过入参实体修改数据
     */
    @Test
    public void updateByEntity() {
        Pig pig = new Pig();
        pig.setId(1L);
        pig.setName("猪猪改名字2");

        var pigList = pignoo.writer(Pig.class);
        pigList.mixById(pig);// mix 混合：根据ID匹配已有元素，忽略入参中的null，只更新非空。也就是将旧数据和新数据混合在一起
        pigList.replaceById(pig);// replace 替换：根据ID匹配已有元素，用新实体完全替换旧的数据，NULL也会更新

        Pig pig2 = new Pig();
        pig2.setName("老猪改名字");
        pigList.filter(Pig::getAge, ">", 5);
        pigList.mixAll(pig2);// 混合所有满足条件的数据，ID不会被更新
        pigList.replaceAll(pig2);// 替换所有满足条件的数据，ID不会被更新

        // 通过入参实体无法更新ID，想要更新ID，使用先查询后更新的方式
        var pigList2 = pignoo.writer(Pig.class);
        Pig maxIdPig = pigList2.sort(Pig::getId, SMode.MAX_FIRST).getOne();
        maxIdPig.setId(maxIdPig.getId() + 1);
    }

    /**
     * 删除和修改类似，可以先查询再删除，或者通过ID（实体）删除
     */
    public void delete() {
        var pigList = pignoo.writer(Pig.class);

        // 按条件删
        pigList.copyWriter().filter(Pig::getId, "!=", 1L)
                .filter(Pig::getAge, ">", 5)
                .removeAll();

        // 查询出一部分数据，再按ID删
        var deletePigs = pigList.copyWriter().filter(Pig::getId, "!=", 1L)
                .sort(Pig::getId, SMode.MAX_FIRST)
                .get(0, 2);
        pigList.copyWriter().filter(Pig::getId, "in", deletePigs.stream().map(Pig::getId).toList())
                .removeAll();

        // 同上面，删除时候通过直接传入的方式实体携带ID
        pigList.copyWriter().filter(Pig::getId, "!=", 1L)
                .sort(Pig::getId, SMode.MAX_FIRST)
                .get(0, 2)
                .stream().forEach(p -> {
                    pigList.removeById(p);
                });;

    }

}
