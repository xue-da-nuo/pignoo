# PIGNOO

**Pignoo**是一个用于应对大多数小型Java项目的轻量ORM框架。Pignoo首页：<https://www.xuesinuo.com/#/pignoo>

![pignoo](https://www.xuesinuo.com/img/pignoo.png)

## Pignoo的设计

### 核心理念：用类List操作取代数据库操作

先看一段代码：

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
    private Pignoo pignoo = new BasePignoo(Test.dataSource);

    public void test() {
        var list = pignoo.writer(Pig.class);
        List<Pig> pigs = list.getAll();// 查询
        System.out.println(pigs);

        new Pig(null, "新的小猪");
        Pig newPig = list.add(pig);// 新增

        newPig.setName("小猪改名");// 修改

        list.filter(Pig::getId, "==", newPig.getId()).removeAll();// 删除
    }
}
```

上面代码，诠释了Pignoo的核心设计思路：**用类似List与对象的操作取代数据库操作**，以此极致简化CRUD编码复杂度。

在纯粹的Pignoo模式下，你甚至可以忽视数据库的存在，数据库只是一种将List持久化的载体，一切操作回归List。

### 名字的由来

Pignoo - 名字来源于《Despicable Me 3》中Minions（小黄人）向Gru（格鲁）抗议生活无聊时喊的词。可能这个词本身没啥意义，但Bilibili上字母打出的“Pignoo（无聊）”。正巧注意到这个词那天，本项目开始动工。Pignoo这个单词好记又没有任何特指意义，蛮适合用来起名字的。又因为这个词拼写接近Pig，于是项目Logo形象是个戴着Minions同款护目镜的小猪🐷。

Gru（格鲁 - 《Despicable Me》主人公，主宰一切的人）。记住这两个名字，这是Pignoo框架中的两个核心类。

### 局限性

Pignoo不是一个全功能的ORM框架，它可能满足不了你100%对数据库操作的要求。但Pignoo可以在大部分简单数据操作场景，简化你的代码。

好在Pignoo侵入性极低，可以轻松和其他主流ORM框架共存，使用其他ORM以弥补Pignoo空缺的功能。

所以，Pignoo不适合在业务复杂型项目中使用。Pignoo可以快速开发小型项目，也可以在中大型项目里，用于开发独立的业务简单型号模块。

### 设计思路

#### 对象及是数据

Pignoo的核心思路是：用List与对象的操作取代数据库操作。操作对象，就等同于操作数据库。比如上面`newPig.setName("小猪改名");`满足了：

- newPig是从Pignoo提供的“list”中取出
- set操作修改了newPig的属性

于是，“小猪改名”就会被按照ID，更新到数据库中。但注意**多线程修改数据时，Pignoo不能实时将其他线程更新的结果写到当前线程已经查询出的对象上**。

弥补这点问题，一般我们多线程对同一个对象进行操作时，会通过数据库事务保证一致性。

#### 用List或Stream操作的眼光看待Pignoo

Pignoo对数据的操作，很像是在使用list与list.stream()。在分解代码逻辑时，可以抛开SQL思维，用List去理解Pignoo的逻辑。例如：

```java
    pignoo.writer(Pig.class) // 获取到Pignoo的PigList
        .sort(Pig::getId, SMode.MIN_FIRST) // 按照ID从小到大排序
        .sort(Pig::getName, SMode.MIN_FIRST) // 按照Name字典序从前到后排序
        .getAll(); // 查询最终结果是：先按Name字典序排序，同名时再按ID从小到大排序
```

如果你还是在用SQL思路看待问题，很可能会误认为上面代码是“先按ID排序，再按Name排序”。但设想一下，你如果是用同样的方式`.stream()`操作List，第二次排序时候，会将第一次的排序结果打乱，最终形成Name顺序优先、ID顺序其次的结果。

#### 程序设计理念

Pignoo的目标：准确性 > 易用性 > 性能。在功能准确的前提下，操作易用性更为优先，但是这不代表Pignoo的性能非常差。

Pignoo是基于**标准JavaBean**、**JDBC**、**DataSource**、**Slf4j**、**SpringAOP**的：

- 标准JavaBean（但无需序列化接口）：Pignoo操作的数据必须都是标准的JavaBean对象，推荐Lombok的@Data注解。
- JDBC：目前还没有考虑兼容Reactive访问方式，一切的根基还是传统JDBC。
- DataSource：Pignoo不提供DataSource的构建，需要使用者提供一个DataSource，当然推荐DataSource可以是通过连接池构建的。
- Slf4j：Pignoo的日志是基于Slf4j的，例如输出com.xuesinuo.pignoo下的debug日志可以看到SQL执行情况。
- SpringAOP：Pignoo中用到了CGLIB动态代理，SpringAOP作为优秀的AOP框架，Pignoo基于它实现的动态代理。
- JDK21：其实Pignoo没有用到很新的特性。如果你很需要，可以issue+邮件来催促降低JDK版本。

#### 动态代理用在哪里？

Pignoo中，有个很关键的设计思路：用List与对象的操作取代数据库操作。为了实现这个构想，从PignooWriter中取出的每个对象，都会增加一层代理，代理监听setter方法并执行SQL操作，从而达到对象操作等于数据操作的效果。这也是Pignoo牺牲性能，换取易用性的提现。

在**Pignoo的作用域**外，setter的代理操作会失效，再做JavaBean的set操作，不会影响数据库。

#### Pignoo类

Pignoo是最重要的核心类（接口），每个Pignoo实例代表一系列数据操作，比如一个数据库事务。并且，一切数据操作都是从pignoo对象开始的。

Pignoo是AutoCloseable的子类，所以标准的pignoo声明要写在try-with-resource中保证pignnoo实例会正确关闭。

写很多的try-with-resource很不利于快速开发，pignoo提供了Gru和Pignoo-Spring来解决它。虽然不需要写关闭，但你要知道pignoo代表一系列数据操作，pignoo关闭了，意味着退出了**Pignoo的作用域**。

#### Gru

Gru是Pignoo的管理者，或者理解成Pignoo工厂。使用Gru可以让pignoo隐式关闭，并且Gru增加了事务管理。

在非Spring环境下，或不想使用Spring事务管理的情况下，最优先使用Gru来构建pignoo。例如：

```java
    Gru gru = new Gru(dataSource); // 线程安全，可以为每个dataSource生成一个Gru实例，全局共享

    List<Pig> list = gru.run(pignoo -> {// 非事务操作
        return pignoo.reader(Pig.class).getAll();
    });

    gru.runTransaction(pignoo -> {// 事务操作
        pignoo.writer(Pig.class).getOne().setName("新名字");
    });
```

仅在RuntimeException时，事务会回滚。

#### PignooReader与PignooWriter

通过`pignoo.reader(class);`与`pignoo.writer(class);`方法，都可以得到具有类似List操作的，代表全部此类型数据的集合——PignooReader、PignooWriter。

PignooReader提供只读操作：reader读取的对象变化，不会映射到数据库（读取的对象不具有代理）。

PignooWriter提供读写操作，且writer的读操作是“为了写而准备的”：比reader多出来一些专门用于写的操作；读操作会给读出的行加“数据库写锁”；读取出来的对象变化，会映射到数据库（读取的对象具有代理）。

reader与writer类似Stream，是“惰性求值”的。它们是“惰性提交”——在执行类似`.getOne()`这种明确的数据操作语句时，才会触发SQL操作；`.filter(...)`操作只会缓存过滤条件，不会触发SQL操作。能触发SQL是“终端操作”，不能触发SQL是“中间操作”。他们很好区分，中间操作的返回值是reader或writer本身，中间操作是可以链式调用的。

**注意！**reader与writer不像Stream具有不可变性，**中间操作会影响reader与writer自身**！

reader与writer提供了`.copyReader()`与`.copyWriter()`方法，用于复制一个一样的reader或writer。也可以在每次使用reader、writer前，通过显式调用`.copyReader()`、`.copyWriter()`已达到像Stream一样的不可变效果。

## 引入依赖

Pignoo核心功能：Pignoo与Gru。通过Maven引入：

```xml
<dependency>
    <groupId>com.xuesinuo</groupId>
    <artifactId>pignoo-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

Pignoo-Spring：如果实在Spring环境下，且希望使用Spring的事务管理机制，需额外引入：

```xml
<dependency>
    <groupId>com.xuesinuo</groupId>
    <artifactId>pignoo-spring</artifactId>
    <version>1.0.0</version>
</dependency>
```

Pignoo-AutoDatabase是一个可以根据实体类型，自动管理数据库表结构的Pignoo扩展。请按需引入：

```xml
<dependency>
    <groupId>com.xuesinuo</groupId>
    <artifactId>pignoo-autodatabase</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 实体与注解

### 注解用法

- `@Table`：标记实体类，映射到表。
  - 作用1：却别普通实体。配置中如果配置了必须使用注解，则没有`@Table`的实体类，Pignoo会忽略。
  - 作用2：别名。`@Table`的别名优先级最高，如果不指定别名，则按照配置的映射规则匹配表明。
- `@Column`：标记实体类属性，映射到列。
  - 作用1：却别其他普通属性。
    - 配置中如果配置了必须使用注解，则没有`@Column`的属性，Pignoo会忽略。
    - 如果配置允许没有`@Column`，则可以使用`@Column(ignore=true)`明确忽略。
  - 作用2：别名。`@Column`的别名优先级最高，如果不指定别名，则按照配置的映射规则匹配列名。
  - 作用3：数据规模。
    - 数据规模，及数据库中的数据类型。
    - 同样的Java String可以是`char(6)`、`varchar(100)`、`longtext`
    - `@Column`中有一个“规模（int scale）”属性，它用于声明字段需存储的数据量大小，配合Pignoo-AutoDatabase使用。
  - 作用4：主键。自动（是主键）、非自动（是主键）、不是主键、不确定，4种可选，默认是“不确定”。
    - **Pignoo实体是必须包含单主键的！**
    - 如果声明了是主键，实体上的注解优先
    - 如果不声明是否为主键，Pignoo会根据配置的规则，自动查询主键
    - 如果想让匹配上规则的列，不作为主键，可以声明为“不是主键”
- `@Link`：将本实体映射到其他实体（Class）上。
  - 会优先解析被映射的实体类
  - 然后再解析``@Link`本类的属性
  - 表名与被映射实体类保持一致
  - 列只保留与被映射实体能对应上的字段，其他字段一律忽略，即使标记了`@Column`也可能被忽略

### 实体类的设计哲学

Pignoo对实体的认知有两种方式：**实体是表**、**实体是可持久化的Java数据类型**。“实体是表”很好理解，依然是操作数据库的眼光去看到Pignoo，为实体映射关系上增加各种配置、别名加以控制。但Pignoo的哲学是**弱化数据库的存在**，所以“实体是可持久化的Java数据类型”是Pignoo更推荐的！

1. 实体类上不声明`@Table`，或标注`@Table`但不指定别名，依靠全局规则匹配
2. 一般的实体类属性上不声明`@Column`，如果使用了Pignoo-AutoDatabase，在控制数据规模时使用`@Column(scale=xxx)`
3. 在需要部分查询某个实体类时，通过`@Link`声明一个属性少于原始体类的“视图类”

例如：

```java
@Data
public class User {
    private Long id;
    private String username;
    private String password;
    @Column(scale = Column.PresetScale.LARGE) // 告诉Pignoo-AutoDatabase这是个大文本，创建数据库时使用longtext类型
    private String bigText;
}
```

```java
@Link(User.class)
public class UserResponse {
    private Long id;
    private String username;
    private String token; // token不是User属性，不会被Pignoo做映射
    // password不会被Pignoo查询，防止password泄露
    // bigText是大文本，不必要时不查询，提升查询效率
}
```

## 配置说明

### PignooConfig类

- `engine`：数据库引擎，目前只支持MySQL。不填会自动识别，自动识别需额外消耗一次数据库链接。（😭有时间我一定更新其他数据库的支持，有需要Issue）
- `annotationMode`：注解使用方式。
  - MUST：必须使用注解。无注解的地方，Pignoo会忽略。
  - MIX：（默认）混合使用。无注解的地方，按照配置规则做映射。
- `namingMode`：自动映射命名规则。
  - SAME：类名等于表名，属性名等于列名。（纯Pignoo模式推荐，不用在意数据库命名规范，全由Pignoo管理，这样能支持的Java类名、属性名更自由）
  - CAMEL_TO_SNAKE：（默认）驼峰命名转蛇形命名。
- `primaryKeyNamingConvention`：主键命名规则。
  - 此项配置是一个FunctionalInterface（可以用lambda表达式声明一个命名方法）
  - lambda的入参有表明和类名。一种常见的方式，是用表明加id后缀做主键名：`(tableName, className) -> tableName + "_id"`。
  - 默认是`id`。
- `autoPrimaryKey`：主键是否是自动生成的。默认`true`。

可以看到，Pignoo的默认配置，就是最贴合Pignoo的使用场景的配置————需要构建一个新的、小型的、需要数据存储的Java项目。
