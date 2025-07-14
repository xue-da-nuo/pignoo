package com.xuesinuo.pignoo.demo.core;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooList;
import com.xuesinuo.pignoo.core.PignooFilter.FMode;
import com.xuesinuo.pignoo.core.PignooSorter.SMode;
import com.xuesinuo.pignoo.core.implement.BasePignoo;
import com.xuesinuo.pignoo.demo.table.Pig;

/**
 * 查询案例
 */
@SpringBootTest
public class Demo02_Query {

    public Pignoo pignoo;

    public Demo02_Query(@Autowired DataSource dataSource) {
        pignoo = new BasePignoo(dataSource);
    }

    /**
     * 多次过滤：每次过滤后，List本身都会变为使用了过滤条件的结果
     */
    @Test
    public void filters() {
        PignooList<Pig> pigList = pignoo.getPignooList(Pig.class);

        pigList.filter(Pig::getName, "like", "新猪报道%");
        pigList.getAll();// name like '新猪报道%'

        pigList.filter(Pig::getWeight, ">", new BigDecimal("10"));
        pigList.getAll();// name like '新猪报道%', and then weight > 10
    }

    /**
     * 使用List的赋值，可以生成多个List分别使用
     */
    public void useCopy() {
        PignooList<Pig> list1 = pignoo.getPignooList(Pig.class);
        list1.filter(Pig::getAge, ">", 10);
        PignooList<Pig> list2 = list1.copy();
        list1.filter(Pig::getId, "<=", 10);
        list2.filter(Pig::getId, ">", 10);

        list1.getAll();// age > 10 and id <= 10
        list2.getAll();// age > 10 and id > 10
    }

    /**
     * 较复杂的条件拼接：（如果条件很复杂，无法通过一些简单的过滤条件拼接形成，那就不建议使用Pignoo了）
     * <p>
     * sort排序、filter筛选，调用后可以理解为对原list立即生效！
     * <p>
     * 每次过滤后，会立刻应用好当前条件；and、or即使混合使用，也是从左到右依次执行
     * <p>
     * filter是支持嵌套的，嵌套时，一个filter视为一个整体，套在前一个filter
     */
    @Test
    public void multiFilter() {
        PignooList<Pig> pigList = pignoo.getPignooList(Pig.class);
        pigList.sort(Pig::getAge, SMode.MAX_FIRST);// 先按年龄降序排序
        pigList.filter(Pig::getColor, "like", "%黑%");// 【单条件筛选】仅保留了名字中包含黑字的
        pigList.filter(f -> f.or(Pig::getWeight, ">", 10).or(Pig::getWeight, "<", 2));// 【组合筛选】在前一步的基础上，保留了重量大于10或小于2的
        pigList.sort(Pig::getWeight, SMode.MIN_FIRST);// 再按体重升序排序，这时候排序完，年龄就不一定是降序了，因为这一步仅按体重排序！体重相同的猪之间还是会按照先前的年龄排序。
        pigList.getAll();
    }

    /**
     * 条件的枚举用法
     */
    @Test
    public void filterMode4Enum() {
        PignooList<Pig> pigList = pignoo.getPignooList(Pig.class);
        /*
         * 各类查询条件
         */
        pigList.filter(Pig::getAge, FMode.EQ, 1);// 相等，只能跟1个参数
        pigList.filter(Pig::getAge, FMode.NOT_EQ, 1);// 不相等，只能跟1个参数
        pigList.filter(Pig::getAge, FMode.GT, 1);// 大于，只能跟1个参数
        pigList.filter(Pig::getAge, FMode.LT, 1);// 小于，只能跟1个参数
        pigList.filter(Pig::getAge, FMode.GE, 1);// 大于等于，只能跟1个参数
        pigList.filter(Pig::getAge, FMode.LE, 1);// 小于等于，只能跟1个参数
        pigList.filter(Pig::getName, FMode.LIKE, "新猪报道%");// like，只能跟1个参数
        pigList.filter(Pig::getName, FMode.NOT_LIKE, "新猪报道%");// not like，只能跟1个参数
        pigList.filter(Pig::getAge, FMode.IN, 1, 2, 3);// in，可以跟多个参数
        pigList.filter(Pig::getAge, FMode.NOT_IN, 1, 2, 3);// not in，可以跟多个参数

        /**
         * 集合的用法：支持Iterable（Collection、List、Set）和Array。 除Iterable和Array外，其他参数都视为单一元素
         */
        Collection<Long> ids1 = List.of(1L, 2L, 3L);
        long[] ids2 = new long[] { 4L, 5L, 6L };
        pigList.filter(Pig::getId, FMode.IN, ids1, ids2, 6, 7, null);// in，可以集合、元素混用，支持null
        pigList.filter(Pig::getId, FMode.NOT_IN, ids1, ids2, 6, 7);// not in，可以集合、元素混用，支持null

        /**
         * IS NULL的判断
         */
        Integer id = null;
        pigList.filter(Pig::getId, FMode.EQ, id);// 相等，支持null
        pigList.filter(Pig::getId, FMode.NOT_EQ, id);// 不相等，支持null
        // pigList.filter(Pig::getId, FMode.EQ, null);// 不可直接赋值null，JRE会提示警告：null无法做为可变数组入参，建议使用IS_NULL、IS_NOT_NULL
        pigList.filter(Pig::getId, FMode.IS_NULL);
        pigList.filter(Pig::getId, FMode.IS_NOT_NULL);

        pigList.getAll();
    }

    /**
     * 条件的别名用法：仅支持正确的别名，别名参考{@link FMode#getName()}
     * <p>
     * 原则上，别名必须和FMode中的命名一致
     * <p>
     * 实际上，别名不区分大小写{@link java.lang.String#toLowerCase()}，别名会忽略两段空白字符{@link java.lang.String#trim()}
     * <p>
     * 附：list的sort和filter链式调用，意味着调用返回结果是PignooList。注意：调用会对原List生效，无论是否赋值，原list都被更改了！
     */
    @Test
    public void filterMode4String() {
        PignooList<Pig> list = pignoo.getPignooList(Pig.class);

        list = list.filter(Pig::getAge, "==", 1)// “list = ”赋值是不必要的
                .filter(Pig::getAge, "!=", 1)
                .filter(Pig::getAge, ">", 1)
                .filter(Pig::getAge, "<", 1)
                .filter(Pig::getAge, ">=", 1)
                .filter(Pig::getAge, "<=", 1)
                .filter(Pig::getName, "like", "新猪报道%")
                .filter(Pig::getName, "not like", "新猪报道%")
                .filter(Pig::getAge, "in", 1, 2, 3)
                .filter(Pig::getAge, "not in", 1, 2, 3)
                .filter(Pig::getId, "is null")
                .filter(Pig::getId, "is not null");
        list.getAll();
    }

    /**
     * 从PignooList中获取
     */
    @Test
    public void getSome() {
        PignooList<Pig> pigList = pignoo.getPignooList(Pig.class);
        pigList.filter(Pig::getId, ">", 10);

        pigList.get(5, 10);// id>10的数据，跳过前5个，查询10个
        pigList.getAll();// id>10的数据，查询全部

        pigList.getOne();// id>10的数据，查询第一个，直接拿到对象，可能为NULL
    }

    /**
     * 一些聚合函数，比如求和、求总数、求平均
     */
    @Test
    public void getSum() {
        PignooList<Pig> pigList = pignoo.getPignooList(Pig.class);
        System.out.println(pigList.size());// 求总数
        System.out.println(pigList.sum(Pig::getWeight, BigDecimal.class));// 求和
        System.out.println(pigList.avg(Pig::getAge, Integer.class));// 求平均

        pigList.filter(Pig::getId, ">", 3);
        System.out.println(pigList.size());
    }
}
