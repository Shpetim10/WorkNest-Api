package com.worknest.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Global configuration to enable asynchronous task execution in Spring Boot (e.g., background e-mails).
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
