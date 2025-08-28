# PIGNOO

**Pignoo**是一个用于应对小型Java项目的轻量ORM框架。Pignoo首页：<https://www.xuesinuo.com/#/pignoo>

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
        var list = pignoo.writer(Pig.class);// 加载数据

        List<Pig> pigs = list.getAll();// 查询

        Pig pig = list.add(new Pig(null, "新的小猪"));// 新增

        pig.setName("小猪改名");// 修改

        list.filter(Pig::getId, "==", pig.getId()).removeAll();// 删除
    }
}
```

上面代码诠释了Pignoo的核心设计思路：**用类似List与JavaBean的操作取代数据库操作**，以此极致简化CRUD编码复杂度。

在纯Pignoo模式下，你甚至可以忽视数据库的存在，数据库只是一种将List持久化的载体，一切操作回归List。

### 名字的由来：记住它，方便使用

Pignoo - 名字源于《Despicable Me 3》中Minions（小黄人）向Gru（格鲁）抗议生活无聊时喊的词。可能这个词本身没啥意义，但Bilibili上字幕打出的“Pignoo（无聊）”，正巧那天注意到这个词。Pignoo这个单词好记又没有任何特指意义，蛮适合用来起名字的。又因为这个词拼写接近Pig，于是项目Logo形象是个戴着小黄人同款护目镜的小猪🐷。

Gru（格鲁 - 《Despicable Me》主人公，主宰一切的人）。在Pignoo框架中Gru是数据源载体，是事务的管理器，是Pignoo对象的构造者。

### 局限性

Pignoo不是一个全功能的ORM框架，它可能满足不了你100%对数据库操作的要求。Pignoo只可以在大部分简单数据操作场景中，简化你的代码。

好在Pignoo侵入性极低，可以轻松和其他主流ORM框架共存，使用其他ORM以弥补Pignoo空缺的功能。

依托于Spring的事务管理，Pignoo操作可以在Spring环境下和其他ORM（如JPA、MyBatis）混入同一事务。但不推荐这么做，因为Pignoo的编程风格是为了让你“忘记”这是在操作数据库。

所以，Pignoo不适合在业务复杂型项目中使用。Pignoo可以快速开发小型项目，也可以在中大型项目中较简单的独立模块。

### 设计思路

#### JavaBean及是数据

Pignoo的核心思路是：用List与JavaBean的操作取代数据库操作。操作JavaBean，就等同于操作数据库。比如上面`newPig.setName("小猪改名");`满足了：

- pig是从Pignoo中取出
- setter操作修改了pig的属性

于是，“小猪改名”就会被按照ID，更新到数据库中。但注意**多线程修改数据时，Pignoo不能实时将其他线程更新的结果写到当前线程已经查询出的JavaBean上**。

弥补这点问题，一般我们多线程操作指代同一个行数据的JavaBean进行操作时，会通过数据库事务保证一致性。

#### 用List或Stream操作的眼光看待Pignoo

Pignoo对数据的操作，很像是在使用list与list.stream()。在分解代码逻辑时，可以抛开SQL思维，用List去理解Pignoo的逻辑。例如：

```java
pignoo.writer(Pig.class) // 从Pignoo获取PigList
    .sort(Pig::getId, SMode.MIN_FIRST) // 按照ID排序
    .sort(Pig::getName, SMode.MIN_FIRST) // 按照Name字典序排序
    .getAll(); // 查询最终结果是：先按Name字典序排序，同Name时再按ID从小到大排序
```

如果你还是在用SQL思路看待问题，很可能会误认为上面代码是“先按ID排序，再按Name排序”。但设想一下，你如果是用同样的流程用`.stream()`操作List，第二次排序时候，会将第一次的排序“打乱”，最终形成Name顺序优先、ID顺序其次的结果。

#### 程序设计理念

Pignoo的目标：准确性 > 易用性 > 性能。在功能准确的前提下，操作易用性更为优先，但是这不代表Pignoo的性能非常差：Pignoo只是简单得将JavaBean翻译成了最直接的SQL操作。

Pignoo是基于**标准JavaBean**、**JDBC**、**DataSource**、**Slf4j**、**SpringAOP**的：

- 标准JavaBean（无需Serializable接口）：Pignoo操作的数据必须都是标准的JavaBean，推荐Lombok的@Data注解。
  - 无属性的get方法会失效：Pignoo会反射获取JavaBean的属性，只写get方法则会失效。
  - getter/setter方法规则：（Pignoo采用Lombok的命名方式）boolean类型getter是isXXX，其他类型getter是getXXX。
- JDBC：（目前还没有精力兼容Reactive访问方式）一切的根基是传统JDBC。
- DataSource：Pignoo不提供DataSource的构建，需要使用者提供一个DataSource，当然推荐DataSource可以是通过连接池构建的。
  - DataSource对象如果是通过代理模式构建的动态切换数据源，Pignoo可以正常工作，前提是他们的数据库引擎是相同的。
  - Pignoo允许指定数据库引擎，或在初次使用数据源时自动获取数据库引擎。并在实例中缓存引擎设置。
  - 推荐给每个真实DataSource构建Gru或Pignoo，最好不要用DataSource代理构建Pignoo实例。
- Slf4j：Pignoo的日志是基于Slf4j的，例如输出com.xuesinuo.pignoo下的debug日志可以看到SQL执行情况。
- SpringAOP：Pignoo中用到了CGLIB动态代理，SpringAOP作为优秀的AOP框架，Pignoo基于它实现的动态代理。
- JDK21：Pignoo没有依赖很新的特性。如果你很需要，可以issue来催促降低JDK版本。

#### 动态代理用在哪里？

Pignoo中，有个很关键的设计思路：用List与JavaBean的操作取代数据库操作。为了实现这个构想，从PignooWriter中取出的每个JavaBean，都会增加一层代理，代理监听setter方法并执行SQL操作，从而达到操作JavaBean等于操作数据库的效果。这也是Pignoo牺牲性能，换取易用性的提现。

注意：在**Pignoo的作用域**外，setter的代理操作会失效，再做JavaBean的set操作，不会影响数据库。

注意的注意：Pignoo-Spring中非事务操作的Pignoo作用域延伸至整个线程声明周期，这意味着非事务中用PignooWriter查询JavaBean，传递到如SpringMvc Interceptor，被Interceptor调用setter方法，也是会影响数据库的！

> PignooWriter的查询操作涉及到Pignoo的一个设计理念：writer的一切操作都是为写数据而准备的。利用writer先读后写，就应该放入事务中，否则与其他线程同同时操作，会出现一致性问题。writer放入事务也可以在Spring环境下很好得屏蔽掉作用域失控的问题。

#### Pignoo类

Pignoo是最重要的核心类（接口），每个Pignoo实例代表一组数据操作，比如一个数据库事务。并且，一切数据操作都是从Pignoo对象开始的。

Pignoo是AutoCloseable的子类，所以标准的Pignoo声明要写在try-with-resource中保证Pignoo实例会正确关闭。

大量的try-with-resource很不利于快速开发，Pignoo提供了Gru和Pignoo-Spring来解决它。虽然不需要显式关闭，但你要知道Pignoo实例代表一组数据操作，Pignoo实例关闭了，意味着退出了**Pignoo的作用域**，JavaBean的代理功能也会随之失效。

#### Gru

Gru是Pignoo的管理者，或者理解成Pignoo工厂，事务管理器。使用Gru可以让pignoo隐式关闭，并且Gru增加了事务管理。Gru默认仅在RuntimeException时，事务会回滚。

在非Spring环境下，或不想使用Spring事务管理的情况下，最优先使用Gru来构建pignoo。例如：

```java
Gru gru = new Gru(dataSource); // 线程安全，可以为每个dataSource生成一个Gru实例，全局共享

List<Pig> list = gru.run(pignoo -> { // 非事务操作：查询
    var reader = pignoo.reader(Pig.class);
    Pig peppa = reader.copyReader().filter(Pig::getName, "==", "佩奇").getOne();
    return reader.filter(Pig::getId, ">", peppa.getId()).getAll();
});

Pig pig = gru.runTransaction(pignoo -> { // 事务操作
    Pig peppa = pignoo.writer(Pig.class).filter(Pig::getName, "==", "佩奇").getOne();
    peppa.setName("乔治");
    peppa = pignoo.writer(Pig.class).filter(Pig::getName, "==", "Peppa").getOne();
    peppa.setName("George");
    return peppa;
});

pig.setName("佩奇"); // 超出Pignoo作用域，setter操作不会影响数据库
```

#### PignooReader与PignooWriter

PignooReader与PignooWriter是针对具体JavaBean进行操作的操作器。

- 通过`pignoo.reader(class);`方法获取一个数据读取器，可以理解成一个只读的List。
  - PignooReader提供只读操作。
  - reader读取的JavaBean变化，不会映射到数据库（读取的JavaBean不具有代理）。
- 通过`pignoo.writer(class);`方法获取一个数据操作器，可以理解成一个可读写的List。
  - PignooWriter提供读写操作。
  - writer比reader多出来一些用于操作数据的方法。
  - writer的读操作是“为了写而准备的”。
    - 读操作会在读出的数据上加“数据库写锁”，防止其他事务重复读取。
    - 读取出来的JavaBean变化，会映射到数据库（读取的JavaBean具有代理）。
- reader与writer是“惰性提交”的，类似Stream的“惰性求值”。
  - 末端操作：在执行类似`.getOne()`这种明确的数据操作语句时，才会触发SQL操作。
  - 中间操作：在执行类似`.filter(...)`操作时，只会缓存过滤条件，不会触发SQL操作。
  - 区分“末端操作”与“中间操作”很简单：中间操作的返回值是reader或writer本身，是可以链式调用的。
- 注意：**reader与writer不像stream具有不可变性，**中间操作会影响reader与writer自身！
  - reader与writer提供了`.copyReader()`与`.copyWriter()`方法，用于复制一个一样的reader或writer。
  - 也可以在每次使用reader、writer前，通过显式调用`.copyReader()`、`.copyWriter()`已达到像Stream一样的不可变效果。

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
  - 作用1：却别普通实体。配置中如果配置了必须使用注解，则没有`@Table`的实体类，Pignoo可能会忽略。
  - 作用2：别名。`@Table`的别名优先，如果不指定别名，则使用配置的全局映射规则。
- `@Column`：标记实体类属性，映射到列。
  - 作用1：却别其他普通属性。
    - 如果配置了必须使用注解，则没有`@Column`的属性，Pignoo会忽略。
    - 如果配置不必须使用注解，则可以使用`@Column(ignore=true)`明确忽略此属性。
  - 作用2：别名。`@Column`的别名优先，如果不指定别名，则使用配置的全局映射规则。
  - 作用3：数据规模。
    - 数据规模，及数据库中的数据类型。
    - 例如：同样的Java String可以是`char(6)`、`varchar(100)`、`longtext`。
    - `@Column`中有一个“规模（scale）”属性，它用于声明字段需存储的数据量大小，配合Pignoo-AutoDatabase使用。
  - 作用4：主键。
    - 自动（是主键）、非自动（是主键）、不是主键、不确定，4种可选，默认是“不确定”。
    - **Pignoo实体是必须包含单一主键的！**
    - 如果声明了是主键，实体上的注解优先。
    - 如果不声明是否为主键，Pignoo会根据配置的规则，自动查询主键。
    - 如果配置的规则映射到了本列，但在此实体类中不想让它做主键，可以声明为“不是主键”。
- `@Link`：将本实体映射到其他实体（Class）上。例如在B上@Link(A)：
  - 会优先解析A实体类。
  - 然后再解析B实体类。
  - B映射的表名与A保持一致。
  - B中只保留与A能匹配上的字段，其他字段一律忽略，即使标记了`@Column`也会被忽略。

### 实体类的设计哲学

Pignoo对实体类的认知有两种方式：**类是表**、**类是可持久化的Java数据类型**。“类是表”很好理解，依然是操作数据库的眼光去看到Pignoo，为实体类映射关系上增加各种配置、别名加以控制。但Pignoo的哲学是**弱化数据库的存在**，所以“类是可持久化的Java数据类型”是Pignoo更推荐的！

- 实体类上不声明`@Table`依靠全局规则匹配。
  - Pignoo-AutoDatabase可以吧全部的JavaBean聚合在一个包路径下。
  - 如果JavaBean分散在各个包路径下，可以使用`@Table`来标记这个JavaBean是需要持久化的，但不指定别名。
- 一般的实体类属性上不声明`@Column`。
  - 如果使用了Pignoo-AutoDatabase，在控制数据规模时使用`@Column(scale=xxx)`
- 在需要部分查询某个实体类时，通过`@Link`声明一个属性少于原始体类的“视图类”，或“属性的子集”

下面例子就是纯Pignoo模式下的JavaBean，其中不掺杂任何与数据库相关的信息。与数据库相关的映射规则，隐藏在全局配置中，不在业务代码中体现。举例如下：

```java
// @Table // 可有可无的，要看Pignoo-AutoDatabase的使用方式
@Data
public class User {
    private Long id;
    private String username;
    private String password;
    @Column(scale = Column.PresetScale.LARGE) // 可有可无的，要看Pignoo-AutoDatabase的使用方式
    private String bigText;
}
```

```java
@Link(User.class) // 必要的，将UserResponse.class映射到User.class
@Data
public class UserResponse {
    private Long id;
    private String username;
    private String token; // token不是User属性，不会被Pignoo做映射
    // password不会被Pignoo查询，防止password泄露
    // bigText是大文本，不必要时不查询，提升查询效率
}
```

## 配置说明

在构建Pignoo或Gru时，可以指定一个全局配置，例如：

```java
PignooConfig config = new PignooConfig();
config.setEngine(DatabaseEngine.MYSQL);
config.setAnnotationMode(AnnotationMode.MIX);
Pignoo pignoo = new BasePignoo(dataSource, config);
```

```java
PignooConfig config = new PignooConfig();
config.setPrimaryKeyNamingConvention((tableName, className) -> tableName + "_id");
config.setAutoPrimaryKey(false);
Gru gru = new Gru(dataSource, config);
```

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

## TODO List

- [x] SpringAOP替换为ByteBuddy
- [x] 全局配置允许关闭代理
- [x] 扩展数据集合操作
  - [x] getOne改为getAny忽略排序条件的查询（效率更高）
  - [x] getFirst（使用排序条件）
  - [x] pollFirst
  - [x] pollAny
  - [x] containsId
  - [x] containsIds
  - [x] max maxNullAs
  - [x] min minNullAs
  - [x] sum sumNullAs
  - [x] avg avgNullAs
  - [x] count countNullAs
- [ ] 高级的数据集合操作
  - [ ] iterator游标查询，并指定游标跳页数量
  - [ ] spliterator 并行遍历器
  - [ ] forEach
- [ ] 支持PostgreSQL
- [ ] 自定义非标准数据类型映射规则
  - [ ] 替换标准JDBC类型映射规则（显式调用JDBC的getXXX方法代替getObject方法）
  - [ ] 圈定Pignoo默认支持的数据类型范围
  - [ ] 自定义非标准JDBC类型映射规则，例如String映射成List
