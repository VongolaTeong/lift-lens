package com.liftlens.config;

import com.liftlens.insight.InsightProperties;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables {@code @Scheduled} jobs, the detector thresholds, and a system Clock (overridable in tests). */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(InsightProperties.class)
public class SchedulingConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
