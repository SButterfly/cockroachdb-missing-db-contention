package com.example.cockroachdbdemo.repository.retry;

import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.classify.BinaryExceptionClassifierBuilder;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.BinaryExceptionClassifierRetryPolicy;
import org.springframework.transaction.TransactionSystemException;

import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * A policy, that is based on {@link BinaryExceptionClassifierRetryPolicy}.
 * A more right way would be to extend the {@link BinaryExceptionClassifier}
 * but this approach leaves a fancy builder {@link BinaryExceptionClassifierBuilder};
 */
public class TransientExceptionClassifierRetryPolicy extends BinaryExceptionClassifierRetryPolicy {

    public static final String TRANSIENT_CODE = "40001";

    public TransientExceptionClassifierRetryPolicy(
        BinaryExceptionClassifier exceptionClassifier) {
        super(exceptionClassifier);
    }

    /**
     * Returns true if BinaryExceptionClassifierRetryPolicy returns true
     * or throwable has specific SQLState
     *
     * @param context the current retry status
     * @return true if the operation can proceed
     */
    @Override
    public boolean canRetry(RetryContext context) {
        return super.canRetry(context)
            || Optional.ofNullable(context.getLastThrowable())
            .map(this::isTransientException)
            .orElse(true);
    }

    /**
     * Returns true if throwable has specific SQLState
     *
     * @param throwable exception to check
     * @return true if is TransientException
     */
    private boolean isTransientException(Throwable throwable) {

        // based on https://github.com/cockroachlabs/roach-data/blob/master/roach-data-jpa/src/main/java/io/roach/data/jpa/RetryableTransactionAspect.java
        Optional<SQLException> sqlException = tryGetSQLExceptionCause(throwable);

        return sqlException.map(SQLException::getSQLState)
            .map(TRANSIENT_CODE::equals)
            .orElse(false);
    }

    /**
     * Returns SQLException if it can be traversed in cause.
     * Optional.empty() otherwise
     *
     * @param throwable exception to check
     * @return Optional SQLException if any traversed
     */
    private Optional<SQLException> tryGetSQLExceptionCause(Throwable throwable) {
        if (throwable instanceof TransientDataAccessException
            || throwable instanceof TransactionSystemException) {  // TX abort on commit's
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(throwable);
            if (cause instanceof SQLException) {
                SQLException sqlException = (SQLException) cause;
                // Transient error code
                return Optional.of(sqlException);
            }
        }

        if (throwable instanceof UndeclaredThrowableException) {
            UndeclaredThrowableException exception = (UndeclaredThrowableException) throwable;
            Throwable undeclaredThrowable = exception.getUndeclaredThrowable();
            while (undeclaredThrowable instanceof UndeclaredThrowableException) {
                undeclaredThrowable = ((UndeclaredThrowableException) undeclaredThrowable).getUndeclaredThrowable();
            }

            Throwable cause = NestedExceptionUtils.getMostSpecificCause(undeclaredThrowable);
            if (cause instanceof SQLException) {
                SQLException sqlException = (SQLException) cause;
                return Optional.of(sqlException);
            }
        }

        return Optional.empty();
    }
}
