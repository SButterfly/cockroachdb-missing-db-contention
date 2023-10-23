package com.example.cockroachdbdemo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.CockroachContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ContextConfiguration(initializers = {AbstractDBIntegrationTest.Initializer.class})
public abstract class AbstractDBIntegrationTest {

    private static final DockerImageName DEFAULT_IMAGE_NAME =
        DockerImageName.parse("cockroachdb/cockroach");
    private static final String TAG = "latest-v23.1";

    /*
     * CockroachContainer doesn't allow to set
     * Username/password/schema in advance.
     * Initialize container first and eventually define datasource.
     */
    static class Initializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            CockroachContainer cockroachContainer = new CockroachContainer(DEFAULT_IMAGE_NAME.withTag(TAG));
            cockroachContainer.start();

            TestPropertyValues.of(
                "spring.datasource.url=" + cockroachContainer.getJdbcUrl(),
                "spring.datasource.username=" + cockroachContainer.getUsername(),
                "spring.datasource.password=" + cockroachContainer.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }
}
