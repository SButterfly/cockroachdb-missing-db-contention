package com.example.cockroachdbdemo.repository.retry;

import org.springframework.lang.Nullable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class AutoRetryTransactionTemplate extends TransactionTemplate {

    private final RetryTemplate retryTemplate;

    public AutoRetryTransactionTemplate(
        PlatformTransactionManager platformTransactionManager, RetryTemplate retryTemplate) {
        super(platformTransactionManager);
        this.retryTemplate = retryTemplate;
    }

    @Override
    @Nullable
    public <T> T execute(TransactionCallback<T> action) throws TransactionException {
        return retryTemplate.execute(arg0 -> super.execute(action));
    }
}
