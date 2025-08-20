package com.xuesinuo.pignoo.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 映射到另一个主实体：在纯对象模式下，用于把操作映射到另一个对象上
 * <p>
 * Map to another main entity: in pure object mode, used to map operations to another object
 * <p>
 * Link等价于使用@Table注解的同时，按照目标实体进行解析表明，按照本实体解析列，并且只映射和目标实体相同的列
 * <p>
 * Link is equivalent to using the @Table annotation at the same time, parsing the table name according to the target entity, parsing the column according to this entity, and only mapping and the
 * target entity the same column
 * 
 * @author xuesinuo
 * @since 0.2.4
 * @version 0.2.4
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Link {
    Class<?> value();
}
