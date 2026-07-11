package com.itimizer.jena.transactionalmanager;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link TransactionRunner} backed by a {@code @Transactional} method, so the action runs in a
 * single committed transaction (or rolls back on a runtime exception).
 */
@Component
public class TransactionRunnerImpl implements TransactionRunner {

    @Override
    @Transactional
    public <T> T doInTransaction(TransactionAction<T> action) {
        return action.get();
    }
}
