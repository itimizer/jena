package com.itimizer.jena.transactionalmanager;

import java.util.function.Supplier;

/**
 * A unit of work to execute inside a transaction, returning a result.
 *
 * @param <T> the result type
 */
public interface TransactionAction<T> extends Supplier<T> {
}
