package com.xuesinuo.pignoo.spring;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooList;

/**
 * 基于Spring数据源管理的Pignoo实现
 * <p>
 * Pignoo implementation based on Spring data source management
 * <p>
 * 使用方法：构建一个SpringPignoo类型的SpringBean，在需要的地方直接注入Pignoo接口即可
 * <p>
 * Usage: build a SpringPignoo type SpringBean, and directly inject the Pignoo interface where needed
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
public class SpringPignoo implements Pignoo {

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'close'");
    }

    @Override
    public <E> PignooList<E> getList(Class<E> c) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getList'");
    }

    @Override
    public boolean hasClosed() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hasClosed'");
    }

}
