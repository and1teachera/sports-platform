package com.sportsplatform.league.infrastructure.persistence;

import com.sportsplatform.league.domain.UnitOfWork;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Binds the domain's {@link UnitOfWork} port to a Spring {@link TransactionTemplate}: the work
 * runs in one database transaction and rolls back on an unchecked exception.
 */
@Component
public class TransactionalUnitOfWork implements UnitOfWork {

    private final TransactionTemplate transactionTemplate;

    public TransactionalUnitOfWork(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public <T> T execute(Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }
}
