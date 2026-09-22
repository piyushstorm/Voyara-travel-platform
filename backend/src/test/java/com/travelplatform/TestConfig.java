package com.travelplatform;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * Test configuration. Redis auto-configuration is excluded in test application.yml.
 * OtpService gracefully degrades to in-memory storage when Redis is unavailable.
 */
@TestConfiguration
public class TestConfig {
    // No special beans needed - OtpService handles missing Redis gracefully
}
