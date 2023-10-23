package com.example.cockroachdbdemo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.CockroachContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ContextConfiguration(initializers = {AbstractDBIntegrationTest.Initializer.class})
public abstract class AbstractDBIntegrationTest {
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("cockroachdb/cockroach:latest-v23.1");

//            Uncomment, if you want to test with posgresql
//    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("postgres:latest");

    /*
     * CockroachContainer doesn't allow to set
     * Username/password/schema in advance.
     * Initialize container first and eventually define datasource.
     */
    static class Initializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            var container = new CockroachContainer(DEFAULT_IMAGE_NAME);
//            Uncomment, if you want to test with posgresql
//            var container = new PostgreSQLContainer(DEFAULT_IMAGE_NAME);
            container.start();

            TestPropertyValues.of(
                    "spring.datasource.url=" + container.getJdbcUrl(),
                    "spring.datasource.username=" + container.getUsername(),
                    "spring.datasource.password=" + container.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }
}
