package com.xuesinuo.pignoo.core.exception;

/**
 * 映射异常
 * <p>
 * Mapper exception
 * 
 * @author xuesinuo
 * @since 1.0.0
 * @version 1.0.0
 */
public class MapperException extends RuntimeException {
    public MapperException(Throwable throwable) {
        super(throwable);
    }

    public MapperException(String string) {
        super(string);
    }

    public MapperException(String string, Throwable throwable) {
        super(string, throwable);
    }
}
