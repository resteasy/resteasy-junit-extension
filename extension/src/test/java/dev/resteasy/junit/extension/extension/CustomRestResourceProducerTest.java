/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineTestKit;

import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.api.RestResourceProducer;
import dev.resteasy.junit.extension.extension.resources.TestApplication;

/**
 * Tests custom RestResourceProducer SPI using JUnit's EngineTestKit.
 * <p>
 * This test demonstrates how to implement a custom {@link RestResourceProducer} that can inject custom types
 * into test classes. In a real application, producers must be registered via ServiceLoader
 * in {@code META-INF/services/dev.resteasy.junit.extension.api.RestResourceProducer}.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class CustomRestResourceProducerTest {

    /**
     * Example custom producer that injects String values.
     * <p>
     * In a real application, you would register this in
     * {@code META-INF/services/dev.resteasy.junit.extension.api.RestResourceProducer} like:
     * </p>
     */
    public static class CustomStringProducer implements RestResourceProducer {

        @Override
        public boolean canInject(final ExtensionContext context, final Class<?> clazz,
                final Annotation... qualifiers) {
            // This producer can inject String types
            return String.class.equals(clazz);
        }

        @Override
        public Object produce(final ExtensionContext context, final Class<?> clazz,
                final Annotation... qualifiers) throws IllegalArgumentException {
            if (!canInject(context, clazz, qualifiers)) {
                throw new IllegalArgumentException("Cannot produce type: " + clazz);
            }
            return "Custom produced string";
        }
    }

    /**
     * Integration test that runs a test class with a custom producer using EngineTestKit.
     * This demonstrates end-to-end custom injection by running an actual test class.
     */
    @Test
    public void customProducerIntegration() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(TestClassUsingCustomProducer.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats
                        .started(2)
                        .succeeded(2)
                        .failed(0));
    }

    /**
     * Test class that uses a custom producer for String injection.
     * This is executed via EngineTestKit to demonstrate the SPI mechanism.
     * <p>
     * Note: This test class is not run directly - it's executed by the {@link #customProducerIntegration()} test
     * using EngineTestKit to verify that custom producers work end-to-end.
     * </p>
     */
    @RestBootstrap(application = TestApplication.class)
    public static class TestClassUsingCustomProducer {

        @RestResource
        private String customStringField;

        @Test
        public void fieldInjection() {
            Assertions.assertNotNull(customStringField, "Custom string should be injected into field");
            Assertions.assertEquals("Custom produced string", customStringField,
                    "Field should contain custom producer value");
        }

        @Test
        public void parameterInjection(@RestResource final String customStringParam) {
            Assertions.assertNotNull(customStringParam, "Custom string should be injected as parameter");
            Assertions.assertEquals("Custom produced string", customStringParam,
                    "Parameter should contain custom producer value");
        }
    }
}
