# PIGNOO

Pignoo是一个应对大部分小型Java项目的轻量JDBC框架。

通过Maven引入：

```xml
<dependency>
    <groupId>com.xuesinuo</groupId>
    <artifactId>pignoo-core</artifactId>
    <version>0.2.1</version>
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
        PignooList<Pig> pigList = pignoo.getList(Pig.class);
        List<Pig> pigs = pigList.getAll();// 查询
        System.out.println(pigs);

        new Pig(null, "新的小猪");
        Pig newPig = pigList.add(pig);// 新增

        newPig.setName("小猪改名");// 修改

        pigList.filter(Pig::getId, "==", newPig.getId()).removeAll();// 删除
    }
}
```

上面已经是一个引入Pignoo后，完整的JDBC案例，在此基础上，你还需要额外构建出所需的DataSource，就不再需要做其他事，完成数据库操作。

可以看出，Pignoo的数据库操作，完全符合Java操作List的思路，虽然舍弃了很多SQL的高级功能，但是在小型项目中，这很实用。如果你觉得这种操作数据的方式还不错，认真看完我给出的demo项目，防止到一些坑。

## Pignoo设计

Pignoo的核心思路是：用List与对象的操作取代数据库操作。操作对象，就等同于操作数据库。比如上面`newPig.setName("小猪改名");`满足了：

- newPig是从PignooList中取出的
- set操作修改了newPig的属性

操作了PignooList中的对象等同于操作了数据库表中的数据。PignooList=SQL表，Object=SQL行。

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

在使用PignooList时，请按照使用List的操作直觉来使用它。我来举一个例子：

```java
    pignoo.getList(Pig.class)
        .sort(Pig::getId, SMode.MIN_FIRST);// 按照ID从小到大排序
        .sort(Pig::getName, SMode.MIN_FIRST);// 按照Name字典序从前到后排序
        .getAll();// 查询最终结果是：先按Name字典序排序，同名时再按ID从小到大排序
```

如果你还是在用SQL思路看待问题，你会认为上面代码是“先按ID排序，再按Name排序”。但设想一下，你如果是用同样的方式`.stream()`操作List，第二次排序时候，会将第一次的排序结果打乱！PignooList也是这个思路，最终结果会优先最后指定的排序规则。

## 动态代理用在哪里

Pignoo中，有个很关键的设计思路：用List与对象的操作取代数据库操作。为了实现这个构想，从PignooList中取出的每个对象，都会增加一层代理，代理监听setter方法并执行SQL操作，从而达到对象操作等于数据操作的效果。这也是Pignoo牺牲性能，换取易用性的提现。

Pignoo的**作用域**外，setter的代理操作会失效。方便将对象传出Pignoo作用范围后，再做其他set操作，不会影响数据库。

## 为啥叫Pignoo

Pignoo - 小黄人语的“无聊”。《卑鄙的我3》中小黄人们高呼“Pignoo”抗议格活过于无聊。
开始写Pignoo的时候，那天正好陪儿子在看《卑鄙的我3》，觉得这段小黄人很有趣，“Pignoo Pignoo Pignoo”的口号也很洗脑。并且网上查不到“Pignoo”这个拼写的单词，用它命名即上头又不跟其他存在单词冲突，是个好名字。

通过这个小故事，如果你能记住它，那么在使用过程中，就更容易上手。

另外：Pignoo中自带了一个基于事务的Pignoo构造器：Gru，起名也是源于《卑鄙的我》主人公格鲁。

真的把Pignoo付诸于实践，代码大概是这样：

```java
    List<Pig> pigList = gru.run(pignoo -> {// 非事务操作
        return pignoo.getList(Pig.class).getAll();
    });

    gru.runTransaction(pignoo -> {// 事务操作
        pignoo.getList(Pig.class).getOne().setName("新名字");
    });
```

## v0.2.1

做出如下更新：

- SQL执行器使用connGetter与connCloser来进行连接的关闭，实际关闭逻辑由连接提供者（Pignoo实现）决定
- Spring事务管理对接完成
- pignoo关闭时，释放掉与数据库连接、数据源的指针，可以在Pignoo对象未销毁的情况下，允许回收连接资源

## v0.2.0

做出如下更新：

- 更改了映射规则，支持了自动表名、字段名映射，支持多种映射方式配置
- 更改了主键映射方式，默认不标注主键时，提供主键命名规则配置，提供默认主键是否自增配置
- 实体和表一一对应是，可以不使用任何注解了以上都是自动映射规则
- 将非事务情况下的pignoo从持有Connection改为每次SQL重新获取Connection

## v0.1.1

做出如下更新：

- 修复了直接对对象setNull是出现的空指针报错
- 当事务中使用getOne查询时，改用主键加“写锁”
- 一定程度优化SQL拼接，修复filter拼接中的BUG
- 修复了Gru初始化时候的SQL报错BUG

## v0.1.0

这是Pignoo的第一个版本。完成了：

- **核心功能**：List与对象操作代替数据库操作的设计
- 通过注解将JavaBean映射到单表
- PignooList的过滤器
- PignooList的排序功能
- Pignoo的作用域控制
- 基于MySQL语法的SQL生成（目前不支持其他SQL方言）
- Pignoo自己的事务管理机制
- 表必须有主键，且目前不支持联合主键，操作主要以主键为准

v0.1.0注意事项：

- Gru对象和数据源应一一对应
- Pignoo不是单例的，Pignoo代表一次数据连接，关闭连接或将连接返回连接池时，Pignoo也随之关闭
- Pignoo关闭时，也意味着退出了Pignoo作用域，代理对象setter方法操作数据库的功能随之失效
- PignooList是延迟查询的，应用过滤、排序只会缓存当前查询方式，真正get时才会触发真正的查询，类似JDK8的Stream
- 在事务中的查询，会为查询加写锁，这样可以在先查后改的场景避免并发操作导致的一致性问题。易用性 > 性能
- Pignoo不支持事务隔离级别设置、事务传播行为控制，Pignoo甚至不希望Web项目再有Service、Dao分层，拿来List就是干，简单的业务场景下没那么多嵌套
- 如果你需要连接池、如果你需要事务传播行为，这些交给其他框架。例如Pignoo后续做了Spring事务控制的兼容，这些问题就由Spring来解决吧
- 现在虽然没有接入Spring的事务管理，但可以使用Spring的DataSource接入Gru，使用Pignoo的事务管理

## 开发计划

- 优化
  - 调整Filter实现架构，优化性能
- 功能
  - 增加一个表结构识别器，自动将代码中的实体映射成表
- 其他可能的扩展（任重而道远，具体做到哪一步，看实际需要吧...）
  - 兼容更多SQL方言
  - 可选择是否使用代理，如果不使用代理，将对象“放入”List时视为插入或更新操作
  - 将Pignoo融入Spring的事务管理，且给出默认的Boot-Pignoo-Starter
  - 支持更多SQL功能
    - 实体嵌套，表关联
    - 联合主键
    - 无主键
    - 其他
  - 兼容Reactive访问方式（Vert.x - API）
