package com.xuesinuo.pignoo.core.entity;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

import lombok.AllArgsConstructor;

/**
 * 方法执行器，用于经典反射与MethodHandle反射的切换，高频场景使用MethodHandle反射性能更优，经典反射兜底。
 * <p>
 * MethodRunner is used to switch between classical reflection and MethodHandle reflection. In high-frequency scenarios, MethodHandle reflection is more optimal, and classical reflection is used as a
 * fallback.
 * 
 * @author xuesinuo
 * @since 1.1.3
 * @version 1.1.3
 */
@AllArgsConstructor
public class MethodRunner {
    private final Method method;
    private final MethodHandle methodHandle;

    public Object run(Object bean, Object... args) throws Throwable {
        if (methodHandle != null) {
            if (args == null || args.length == 0) {// 高频命中，if效率更高
                return methodHandle.invoke(bean);
            } else if (args.length == 1) {
                return methodHandle.invoke(bean, args[0]);
            }
            switch (args.length) {
            case 2:
                return methodHandle.invoke(bean, args[0], args[1]);
            case 3:
                return methodHandle.invoke(bean, args[0], args[1], args[2]);
            case 4:
                return methodHandle.invoke(bean, args[0], args[1], args[2], args[3]);
            case 5:
                return methodHandle.invoke(bean, args[0], args[1], args[2], args[3], args[4]);
            case 6:
                return methodHandle.invoke(bean, args[0], args[1], args[2], args[3], args[4], args[5]);
            case 7:
                return methodHandle.invoke(bean, args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
            case 8:
                return methodHandle.invoke(bean, args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
            case 9:
                return methodHandle.invoke(bean, args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
            case 10:
                return methodHandle.invoke(bean, args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9]);
            }
        }
        return method.invoke(bean, args);
    }
}
