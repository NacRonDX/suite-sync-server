package com.nacrondx.suitesync;

import com.nacrondx.suitesync.config.TestContainersConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Import;

@Import(TestContainersConfiguration.class)
public class TestSuiteSyncApplication {
  public static void main(String[] args) {
    SpringApplication.from(SuiteSyncApplication::main)
        .with(TestContainersConfiguration.class)
        .withAdditionalProfiles("test")
        .run(args);
  }
}
