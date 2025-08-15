# PIGNOO

Pignoo是一个应对大部分小型Java项目的轻量JDBC框架。

**v0版本警告：在1.0.0发布之前，架构设计还处在变化中。0版本的每个发行版仅能确保在当前小版本下是稳定可用的。**
**v1版本发布前，说明文档是片面的、简单的介绍。如要使用，请通过pignoo-demo项目，获取更完整帮助。**

通过Maven引入：

```xml
<dependency>
    <groupId>com.xuesinuo</groupId>
    <artifactId>pignoo-core</artifactId>
    <version>0.3.1</version>
</dependency>
```

Pignoo目标是将数据库操作转变成Java的List操作，使用者犹如操作内存一般操作数据库内容，完成数据库操作思路。

来看几个案例：

```java
@AllArgsConstructor// Lombok - 全参数构造，不必须，只是为了Demo更简洁
@Data// Lombok - 标准JavaBean，Bean是必须的，但可以不使用Lombok
public class Pig {
    private Long id;
    private String name;
}
```

```java
public class Test {
    private static DataSource dataSource;// 通过任何形式构建好数据源，比如HikariCP、DBCP...
    private Pignoo pignoo = new BasePignoo(Test.dataSource);// 使用数据源，可以构建一个Pignoo实例，我们就用它来操作数据库

    public void test() {
        PignooWriter<Pig> writer = pignoo.writer(Pig.class);
        List<Pig> pigs = writer.getAll();// 查询
        System.out.println(pigs);

        new Pig(null, "新的小猪");
        Pig newPig = writer.add(pig);// 新增

        newPig.setName("小猪改名");// 修改

        writer.filter(Pig::getId, "==", newPig.getId()).removeAll();// 删除
    }
}
```

上面已经是一个引入Pignoo后，完整的JDBC案例，在此基础上，你还需要额外构建出所需的DataSource，就不再需要做其他事，完成数据库操作。

可以看出，Pignoo的数据库操作，完全符合Java操作List的思路，虽然舍弃了很多SQL的高级功能，但是在小型项目中，这很实用。如果你觉得这种操作数据的方式还不错，认真看完我给出的demo项目，防止遇到一些坑。

## Pignoo设计

Pignoo的核心思路是：用List与对象的操作取代数据库操作。操作对象，就等同于操作数据库。比如上面`newPig.setName("小猪改名");`满足了：

- newPig是从PignooWriter中取出的
- set操作修改了newPig的属性

操作了PignooWriter中的对象等同于操作了数据库表中的数据。PignooWriter=SQL表，Object=SQL行。

Pignoo是为了应对小型项目，尤其是面向API编程的时候，我们需要快速开发很多制式需求，后续还可能面临需求微调，能够快速理清源代码思路。为了解决这个场景，Pignoo将数据库操作转变成类似java.util.List的操作，整个开发周期无需关心SQL语句，只关注对象操作。

Pignoo的目标：准确性 > 易用性 > 性能。在功能准确的前提下，操作易用性更为优先，但是这不代表Pignoo的性能非常差。

Pignoo是基于**标准JavaBean**、**JDBC**、**DataSource**、**Slf4j**、**SpringAOP**的：

- 标准JavaBean：Pignoo操作的数据必须都是标准的JavaBean对象，推荐Lombok的@Data注解。
- JDBC：目前还没有考虑兼容Reactive访问方式，一切的根基还是传统JDBC。
- DataSource：Pignoo不提供DataSource的构建，需要使用者提供一个DataSource，当然推荐DataSource可以是通过连接池构建的。
- Slf4j：Pignoo的日志是基于Slf4j的，例如输出com.xuesinuo.pignoo下的debug日志可以看到SQL执行情况。
- SpringAOP：Pignoo中用到了CGLIB动态代理，SpringAOP作为优秀的AOP框架，Pignoo基于它实现的动态代理。
- JDK21：其实Pignoo没有用到很新的特性。如果你很需要，可以issue+邮件来催促降低JDK版本。

## 不要以SQL操作的眼光看待Pignoo

在使用PignooWriter时，请按照使用List的操作直觉来使用它。我来举一个例子：

```java
    pignoo.writer(Pig.class)
        .sort(Pig::getId, SMode.MIN_FIRST);// 按照ID从小到大排序
        .sort(Pig::getName, SMode.MIN_FIRST);// 按照Name字典序从前到后排序
        .getAll();// 查询最终结果是：先按Name字典序排序，同名时再按ID从小到大排序
```

如果你还是在用SQL思路看待问题，你会认为上面代码是“先按ID排序，再按Name排序”。但设想一下，你如果是用同样的方式`.stream()`操作List，第二次排序时候，会将第一次的排序结果打乱！PignooWriter也是这个思路，最终结果会优先最后指定的排序规则。

## 动态代理用在哪里

Pignoo中，有个很关键的设计思路：用List与对象的操作取代数据库操作。为了实现这个构想，从PignooWriter中取出的每个对象，都会增加一层代理，代理监听setter方法并执行SQL操作，从而达到对象操作等于数据操作的效果。这也是Pignoo牺牲性能，换取易用性的提现。

Pignoo的**作用域**外，setter的代理操作会失效，再做JavaBean的set操作，不会影响数据库。

## 为啥叫Pignoo

Pignoo - 小黄人语的“无聊”。《卑鄙的我3》中小黄人们高呼“Pignoo”抗议格活过于无聊。
开始写Pignoo的时候，那天正好陪儿子在看《卑鄙的我3》，觉得这段小黄人很有趣，“Pignoo Pignoo Pignoo”的口号也很洗脑。并且网上查不到“Pignoo”这个拼写的单词，用它命名即上头又不跟其他存在单词冲突，是个好名字。

通过这个小故事，如果你能记住它，那么在使用过程中，就更容易上手。

另外：Pignoo中自带了一个基于事务的Pignoo构造器：Gru，起名也是源于《卑鄙的我》主人公格鲁。

真的把Pignoo付诸于实践，代码大概是这样：

```java
    List<Pig> list = gru.run(pignoo -> {// 非事务操作
        return pignoo.reader(Pig.class).getAll();
    });

    gru.runTransaction(pignoo -> {// 事务操作
        pignoo.writer(Pig.class).getOne().setName("新名字");
    });
```

## v1.0.0前准备

- 完善测试用例
- 完善使用文档
