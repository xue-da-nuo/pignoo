package com.xuesinuo.pignoo.entity;

import java.lang.reflect.Method;
import java.util.function.Function;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

public class FunctionNameGetter<E> {
    private static class NamePicker {
        private String name;
    }

    private E proxy;
    private NamePicker namePicker;

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

    public String getFunctionName(Function<E, ?> fun) {
        String functionName;
        synchronized (this) {
            fun.apply(proxy);
            functionName = namePicker.name;
        }
        return functionName;
    }
}