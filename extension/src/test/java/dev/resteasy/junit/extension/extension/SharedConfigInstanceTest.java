/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.ws.rs.SeBootstrap.Configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.extension.resources.TestApplication;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RestBootstrap(TestApplication.class)
public class SharedConfigInstanceTest {
    private static final AtomicReference<Configuration> CONFIGURATION = new AtomicReference<>();

    @Inject
    private static Configuration STATIC_CONFIGURATION;

    @Inject
    private Configuration instanceConfiguration;
    private final Configuration constructorConfiguration;

    public SharedConfigInstanceTest(final Configuration constructorConfiguration) {
        this.constructorConfiguration = constructorConfiguration;
    }

    @BeforeAll
    public static void beforeAll(final Configuration configuration) {
        CONFIGURATION.set(configuration);
    }

    @Test
    public void checkInstanceSet() {
        Assertions.assertNotNull(CONFIGURATION.get());
    }

    @Test
    public void checkParameter(final Configuration configuration) {
        Assertions.assertNotNull(configuration);
        checkInstance(CONFIGURATION.get(), configuration);
        checkInstance(STATIC_CONFIGURATION, configuration);
        checkInstance(instanceConfiguration, configuration);
        checkInstance(constructorConfiguration, configuration);
    }

    @Test
    public void checkInjectedInstances() {
        Assertions.assertNotNull(constructorConfiguration);
        final var configuration = CONFIGURATION.get();
        checkInstance(STATIC_CONFIGURATION, configuration);
        checkInstance(instanceConfiguration, configuration);
        checkInstance(constructorConfiguration, configuration);

    }

    private void checkInstance(final Configuration config1, final Configuration config2) {
        Assertions.assertEquals(config1, config2,
                () -> String.format("Configuration %s does not equal configuration %s", config1, config2));
        // Check the objects identity are equal
        Assertions.assertEquals(System.identityHashCode(config1), System.identityHashCode(config2), () -> String.format(
                "The identity hash code of %d for configuration %s does not match the identity hash code of %d for configuration %s",
                System.identityHashCode(config1), config1, System.identityHashCode(config2), config2));
    }
}
