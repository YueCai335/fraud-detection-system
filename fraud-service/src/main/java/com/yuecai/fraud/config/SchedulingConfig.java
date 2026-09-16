package com.yuecai.fraud.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Timers for the batch worker. Tests set {@code scheduling.enabled=false} and drive the worker by hand. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
