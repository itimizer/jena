package com.itimizer.jena.transactionalmanager;

/**
 * Runs a {@link TransactionAction} within a Spring-managed transaction. Lets the comment-sparse
 * service layer wrap a block transactionally without each method being {@code @Transactional}
 * itself.
 */
public interface TransactionRunner {

    /** Executes the action in a transaction and returns its result. */
    <T> T doInTransaction(TransactionAction<T> action);
}
