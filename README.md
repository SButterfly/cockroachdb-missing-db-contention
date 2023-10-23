cockroachdb-missing-db-contention
----

Demo repository to reproduce https://github.com/cockroachdb/cockroach/issues/112856

# How to reproduce the issue
```bash
./mvnw test -Dtest=UserJdbcRepositoryTest#saveDeleteSaveInParallelTransaction
```
The test should fail with 'DuplicateKey'

Note! If you replace cockroachDB with postgresql (search by 'Uncomment'), the test will be successful.

# Technical information

Before each test we:
- Start cockroachDb in docker container (AbstractDBIntegrationTest)
- Migrate DB schema with flyway scripts (src/main/resources/migration)
- truncate all data in users table

BaseUserRepositoryTest is an abstract class with two implementations:
- UserJdbcRepositoryTest, which runs the tests over Spring JDBC framework
- UserJpaRepositoryTest, which runs the tests over Spring JPA/Hibernate framework
