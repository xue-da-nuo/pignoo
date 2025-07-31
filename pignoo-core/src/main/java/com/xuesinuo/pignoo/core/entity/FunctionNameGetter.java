package com.xuesinuo.pignoo.core.entity;

import java.lang.reflect.Method;
import java.util.function.Function;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

/**
 * getter方法与属性名映射器
 * <p>
 * 用于通过getter方法获取属性名
 *
 * @param <E> JavaBean Type
 * @author xuesinuo
 * @since 0.1.0
 * @version 0.1.0
 */
public class FunctionNameGetter<E> {
    private static class NamePicker {
        private String name;
    }

    private E proxy;
    private NamePicker namePicker;

    /**
     * 构造方法
     * <p>
     * Constructor
     *
     * @param c 实体类型
     *          <p>
     *          Entity Type
     */
    @SuppressWarnings("unchecked")
    public FunctionNameGetter(Class<E> c) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(c);
        namePicker = new NamePicker();
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                String functionName = method.getName();
                namePicker.name = functionName;
                return null;
            }
        });
        proxy = (E) enhancer.create();
    }

    /**
     * 获取方法名
     * <p>
     * Get Method Name
     *
     * @param fun 方法
     *            <p>
     *            Method
     * @return 方法名
     *         <p>
     *         Method Name
     */
    public String getFunctionName(Function<E, ?> fun) {
        String functionName;
        synchronized (this) {
            fun.apply(proxy);
            functionName = namePicker.name;
        }
        return functionName;
    }
}
