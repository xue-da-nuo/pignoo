package com.xuesinuo.pignoo.core.entity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

/**
 * 查询结果实体的代理的创建器
 * <p>
 * 每个查询结果Entity都会经过此工厂创建代理，代理调用setter方法时会同步更新数据库
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
public class EntityProxyFactory<E> {
    private Enhancer enhancer;
    private EntityMapper<E> mapper;
    /**
     * 正在构建代理时的对象，会调用set方法赋值内容，以此标记此工厂正在构建的代理，正在构建中的代理不执行代理逻辑
     */
    private Object building = null;

    @FunctionalInterface
    public static interface Updater {
        void run(int index, Object setterArg, Object obj);
    }

    public EntityProxyFactory(Class<E> c, EntityMapper<E> mapper, Updater updater) {
        this.mapper = mapper;
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(c);
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                String methodName = method.getName();
                int index = mapper.setterNames().indexOf(methodName);
                if (index >= 0 && method.getParameterCount() == 1 && obj != building) {
                    updater.run(index, args[0], obj);
                }
                return proxy.invokeSuper(obj, args);
            }
        });
        this.enhancer = enhancer;
    }

    @SuppressWarnings("unchecked")
    public synchronized E build(E entity) {
        if (entity == null) {
            return null;
        }
        E proxy = (E) enhancer.create();
        building = proxy;
        for (int i = 0; i < mapper.columns().size(); i++) {
            try {
                if (mapper.setters().get(i) != null && mapper.getters().get(i) != null) {
                    mapper.setters().get(i).invoke(proxy, mapper.getters().get(i).invoke(entity));
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        building = null;
        return proxy;
    }

    public List<E> build(List<E> list) {
        if (list == null) {
            return null;
        }
        return list.stream().map(e -> build(e)).toList();
    }
}