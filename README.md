# PIGNOO

**Pignoo**是一个用于应对小型Java项目的轻量ORM框架。更详细的介绍，请移步Pignoo首页（首次加载可能较慢，请耐心等待）：<https://www.xuesinuo.com/#/pignoo>

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

## TODO List

- [ ] 支持javax.persistence.Id声明数据库主键
- [ ] 支持多组缓存Bean解析，代替static缓存Bean解析；将Bean的解析放入Pignoo运行环境。
- [x] 高级的数据集合操作
  - [x] iterator游标查询，并指定游标跳页数量
  - [ ] spliterator 并行遍历器
  - [x] forEach
- [ ] 自定义非标准数据类型映射规则
  - [ ] 替换标准JDBC类型映射规则（显式调用JDBC的getXXX方法代替getObject方法）
  - [ ] 圈定Pignoo默认支持的数据类型范围
  - [ ] 自定义非标准JDBC类型映射规则，例如String映射成List
- [ ] 支持PostgreSQL
