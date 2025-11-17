package com.nacrondx.suitesync.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfiguration {
  @Bean
  @ServiceConnection
  public PostgreSQLContainer<?> postgresContainer() {
    var container =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    var reuse = System.getProperty("testcontainers.reuse.enable", "false");
    container.withReuse("true".equals(reuse));

    return container;
  }
}
