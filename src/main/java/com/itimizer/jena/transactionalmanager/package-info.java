/**
 * Small helper for running a block of work inside a Spring-managed transaction, keeping the service
 * layer free of per-method {@code @Transactional} annotations.
 */
package com.itimizer.jena.transactionalmanager;
