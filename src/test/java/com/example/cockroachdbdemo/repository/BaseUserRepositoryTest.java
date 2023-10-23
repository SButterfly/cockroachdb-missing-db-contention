package com.example.cockroachdbdemo.repository;

import com.example.cockroachdbdemo.AbstractDBIntegrationTest;
import com.example.cockroachdbdemo.dto.User;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class BaseUserRepositoryTest extends AbstractDBIntegrationTest {

    protected UserRepository userRepository;

    @Autowired
    @Qualifier("transactionTemplate")
    private TransactionTemplate transactionTemplate;

    @Autowired
    @Qualifier("requiresNewTransactionTemplate")
    private TransactionTemplate requiresNewTransactionTemplate;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newCachedThreadPool();
        userRepository = getUserRepository();
        userRepository.truncateAll();
    }

    protected abstract UserRepository getUserRepository();

    @AfterEach
    void tearDown() {
        shutdownExecutionService();
    }

    @Test
    void simpleInsert() {
        var user = user1(UUID.randomUUID());
        userRepository.insert(user);
    }

    @Test
    void testUniqueIndex() {
        var code = UUID.randomUUID();
        var first = user1(code);
        var second = user2(code);

        var saved = userRepository.insert(first);

        Assertions.assertThatThrownBy(() -> {
                    userRepository.insert(second);
                })
                .hasMessageContaining("idx_users_code");
    }

    @Test
    void testSaveDeleteAndSave() {
        var code = UUID.randomUUID();
        var first = user1(code);
        var second = user2(code);

        var saved1 = userRepository.insert(first);
        userRepository.softDelete(saved1.getId());

        var saved2 = userRepository.insert(second);

        Assertions.assertThat(saved2)
                .extracting(it -> it.getId())
                .isNotNull();

        Assertions.assertThat(userRepository.listAll())
                .hasSize(2);
    }

    @Test
    void testSaveDeleteAndSaveInTransactions() {
        var code = UUID.randomUUID();
        var first = user1(code);
        var second = user2(code);

        transactionTemplate.executeWithoutResult(__ -> {
            var saved1 = userRepository.insert(first);
            userRepository.softDelete(saved1.getId());
        });


        var saved2 = transactionTemplate.execute(__ -> userRepository.insert(second));

        Assertions.assertThat(saved2)
                .extracting(it -> it.getId())
                .isNotNull();

        Assertions.assertThat(userRepository.listAll())
                .hasSize(2);
    }

    /**
     * Failing sometimes, that's ok!
     */
    @SneakyThrows
    @RepeatedTest(5)
    void testSaveDeleteSaveInParallel() {
        var code = UUID.randomUUID();
        var first = user1(code);
        var second = user2(code);

        var countDownLatch = new CountDownLatch(2);

        Future<?> future1 = executorService.submit(() -> {
            transactionTemplate.executeWithoutResult(__ -> {
                var saved1 = userRepository.insert(first);
                countDownLatch.countDown();
                userRepository.softDelete(saved1.getId());
            });
        });

        Future<User> future2 = executorService.submit(() -> {
            return transactionTemplate.execute(__ -> {
                countDownLatch.countDown();
                return userRepository.insert(second);
            });
        });

        shutdownExecutionService();

        var saved2 = future2.get();
        Assertions.assertThat(saved2)
                .extracting(it -> it.getId())
                .isNotNull();

        Assertions.assertThat(userRepository.listAll())
                .hasSize(2);
    }

    @Test
    void saveDeleteSaveInNestedTransaction() {
        var code = UUID.randomUUID();
        var first = user1(code);
        var second = user2(code);

        var saved2 = transactionTemplate.execute(_1 -> {
            var saved1 = userRepository.insert(first);

            transactionTemplate.executeWithoutResult(_2 -> {
                var byId = userRepository.findById(saved1.getId())
                                .orElseThrow();

                userRepository.softDelete(byId.getId());
            });

            return userRepository.insert(second);
        });

        Assertions.assertThat(saved2)
                .extracting(it -> it.getId())
                .isNotNull();

        Assertions.assertThat(userRepository.listAll())
                .hasSize(2);
    }

    @Test
    void saveDeleteSaveInParallelTransaction() {
        var code = UUID.randomUUID();
        var first = user1(code);
        var second = user2(code);

        var saved1 = userRepository.insert(first);
        var saved2 = transactionTemplate.execute(_1 -> {
            var user1 = userRepository.findById(saved1.getId());

            // user1 could be empty, if transaction goes to retry due to DB contention
            // in that case we should insery the second transaction only
            user1.ifPresent(byId -> {
                // requiresNewTransactionTemplate will suspend existing transaction
                // and create a new one with a new connection
                requiresNewTransactionTemplate.executeWithoutResult(_2 -> {
                    userRepository.softDelete(byId.getId());
                });
            });

            return userRepository.insert(second);
        });

        Assertions.assertThat(saved2)
                .extracting(it -> it.getId())
                .isNotNull();

        Assertions.assertThat(userRepository.listAll())
                .hasSize(2);
    }

    @SneakyThrows
    private void shutdownExecutionService() {
        final var timeout = 5000;
        executorService.shutdown();
        var finished = executorService.awaitTermination(timeout, TimeUnit.SECONDS);
        if (!finished) {
            throw new RuntimeException("Expected all threads to be finished in " + timeout + " seconds");
        }
    }

    private static User user1(UUID code) {
        return new User()
                .setId(UUID.randomUUID())
                .setCode(code)
                .setName("user1");
    }

    private static User user2(UUID code) {
        return new User()
                .setId(UUID.randomUUID())
                .setCode(code)
                .setName("user2");
    }
}
