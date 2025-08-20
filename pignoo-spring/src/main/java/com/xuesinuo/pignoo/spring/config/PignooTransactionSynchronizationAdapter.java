package com.xuesinuo.pignoo.spring.config;

import org.springframework.transaction.support.TransactionSynchronization;

import com.xuesinuo.pignoo.spring.implement.SpringPignoo;

import lombok.RequiredArgsConstructor;

/**
 * Pignoo事务同步适配器，事务结束时，用于Close Pignoo
 * <p>
 * Pignoo Transaction Synchronization Adapter, used to close Pignoo when the transaction ends
 *
 * @author xuesinuo
 * @since 0.2.1
 * @version 0.2.1
 */
@RequiredArgsConstructor
public class PignooTransactionSynchronizationAdapter implements TransactionSynchronization {

    private final SpringPignoo springPignoo;

    @Override
    public void afterCompletion(int status) {
        springPignoo.closeSpringTransaction();
    }
}
