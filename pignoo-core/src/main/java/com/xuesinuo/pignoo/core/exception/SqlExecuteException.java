package com.xuesinuo.pignoo.core.exception;

/**
 * SQL执行异常
 * <p>
 * SQL execute exception
 * 
 * @author xuesinuo
 * @since 1.0.0
 * @version 1.0.0
 */
public class SqlExecuteException extends RuntimeException {
    public SqlExecuteException(Throwable throwable) {
        super(throwable);
    }

    public SqlExecuteException(String string) {
        super(string);
    }

    public SqlExecuteException(String string, Throwable throwable) {
        super(string, throwable);
    }
}
