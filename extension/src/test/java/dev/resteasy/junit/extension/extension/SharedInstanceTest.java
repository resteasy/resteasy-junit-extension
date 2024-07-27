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
public class SharedInstanceTest {
    private static final AtomicReference<Configuration> CONFIGURATION = new AtomicReference<>();

    @Inject
    private static Configuration STATIC_CONFIGURATION;

    @Inject
    private Configuration instanceConfiguration;
    private final Configuration constructorConfiguration;

    public SharedInstanceTest(final Configuration constructorConfiguration) {
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
        Assertions.assertEquals(CONFIGURATION.get(), configuration);
        Assertions.assertEquals(STATIC_CONFIGURATION, configuration);
        Assertions.assertEquals(instanceConfiguration, configuration);
        Assertions.assertEquals(constructorConfiguration, configuration);
    }

    @Test
    public void checkInjectedInstances() {
        Assertions.assertNotNull(constructorConfiguration);
        final var configuration = CONFIGURATION.get();
        Assertions.assertEquals(STATIC_CONFIGURATION, configuration);
        Assertions.assertEquals(instanceConfiguration, configuration);
        Assertions.assertEquals(constructorConfiguration, configuration);

    }
}
