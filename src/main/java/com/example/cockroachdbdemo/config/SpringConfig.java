package com.example.cockroachdbdemo.config;

import com.example.cockroachdbdemo.repository.retry.AutoRetryTransactionTemplate;
import com.example.cockroachdbdemo.repository.retry.TransientExceptionClassifierRetryPolicy;
import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import java.net.ConnectException;

@Configuration
public class SpringConfig {

    @Bean("transactionTemplate")
    public AutoRetryTransactionTemplate transactionTemplate(PlatformTransactionManager platformTransactionManager) {
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .exponentialBackoff(
                        100,
                        2,
                        5000
                )
                .customPolicy(transactionTemplateRetryPolicy(3))
                .build();

        return new AutoRetryTransactionTemplate(platformTransactionManager, retryTemplate);
    }

    @Bean("requiresNewTransactionTemplate")
    public AutoRetryTransactionTemplate requiresNewTransactionTemplate(
            PlatformTransactionManager platformTransactionManager) {
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .exponentialBackoff(
                        100,
                        2,
                        5000
                )
                .customPolicy(transactionTemplateRetryPolicy(3))
                .build();

        var autoRetryTransactionTemplate = new AutoRetryTransactionTemplate(platformTransactionManager, retryTemplate);
        autoRetryTransactionTemplate.setName("requires_new");
        autoRetryTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return autoRetryTransactionTemplate;
    }

    private RetryPolicy transactionTemplateRetryPolicy(int maxAttempts) {

        //Default policy for number of retries if any occurs
        MaxAttemptsRetryPolicy maxAttemptsRetryPolicy = new MaxAttemptsRetryPolicy(maxAttempts);

        //Simple Exception classifier to Enable Retries on
        BinaryExceptionClassifier binaryExceptionClassifier =
                BinaryExceptionClassifier.builder()
                        .retryOn(ConnectException.class)
                        .traversingCauses()
                        .build();

        //Extended Exception classifier to Enable Retries on specific (cockroachDB recoverable) cases
        TransientExceptionClassifierRetryPolicy transientExceptionClassifierRetryPolicy
                = new TransientExceptionClassifierRetryPolicy(binaryExceptionClassifier);

        /*
         * Composite pattern for policies.
         * Uses pessimistic approach meaning all policies shall
         * return cantRetry() = true, otherwise consider non-retryable or exhausted.
         */
        CompositeRetryPolicy compositeRetryPolicy = new CompositeRetryPolicy();
        compositeRetryPolicy.setPolicies(
                new RetryPolicy[]{
                        maxAttemptsRetryPolicy,
                        transientExceptionClassifierRetryPolicy
                }
        );
        return compositeRetryPolicy;
    }
}
