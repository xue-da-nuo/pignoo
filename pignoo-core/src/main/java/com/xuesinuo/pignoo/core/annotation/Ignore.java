package com.xuesinuo.pignoo.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 忽略属性，在纯对象模式下，用于标记一个属性不做存储
 * <p>
 * Ignore attribute, in pure object mode, used to mark an attribute does not store
 *
 * @author xuesinuo
 * @since 0.2.4
 * @version 0.2.4
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface Ignore {

}
