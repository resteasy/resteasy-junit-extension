/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.SeBootstrap.Configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.extension.resources.TestApplication;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RestBootstrap(TestApplication.class)
public class SharedConfigInstanceTest {
    private static final AtomicReference<Configuration> CONFIGURATION = new AtomicReference<>();

    @RestResource
    private static Configuration STATIC_CONFIGURATION;

    @RestResource
    private Configuration instanceConfiguration;
    private final Configuration constructorConfiguration;

    public SharedConfigInstanceTest(@RestResource final Configuration constructorConfiguration) {
        this.constructorConfiguration = constructorConfiguration;
    }

    @BeforeAll
    public static void beforeAll(@RestResource final Configuration configuration) {
        CONFIGURATION.set(configuration);
    }

    @Test
    public void checkInstanceSet() {
        Assertions.assertNotNull(CONFIGURATION.get());
    }

    @Test
    public void checkParameter(@RestResource final Configuration configuration) {
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

    @Nested
    @RestBootstrap(value = TestApplication.class, configFactory = SecondInstanceConfigurationProvider.class)
    @Disabled("Disable until we can upgrade to RESTEasy 6.2.17.Final")
    class NestedWithBootstrap {
        @RestResource
        private Configuration nestedConfiguration;

        @RestResource
        private Configuration nestedInstanceConfiguration;

        @Test
        public void nestedConfigurationIdentity(@RestResource final Configuration nestedParamConfiguration) {
            Assertions.assertNotNull(nestedConfiguration, "Nested configuration should be injected");
            Assertions.assertNotNull(nestedInstanceConfiguration, "Nested instance configuration should be injected");
            Assertions.assertNotNull(nestedParamConfiguration, "Nested parameter configuration should be injected");

            // Within nested class, all configurations should be the same
            checkInstance(nestedConfiguration, nestedInstanceConfiguration);
            checkInstance(nestedConfiguration, nestedParamConfiguration);

            // Compare with outer class configuration
            System.out.println("Outer config identity: " + System.identityHashCode(STATIC_CONFIGURATION));
            System.out.println("Nested config identity: " + System.identityHashCode(nestedConfiguration));

            // Question: Should nested @RestBootstrap get its own instance or share with parent?
            // This test documents the actual behavior
            if (STATIC_CONFIGURATION == nestedConfiguration) {
                System.out.println("Nested class SHARES Configuration with outer class");
            } else {
                System.out.println("Nested class gets its OWN Configuration (different from outer)");
            }
        }
    }

    @Nested
    class NestedWithoutBootstrap {
        @Test
        public void attemptConfigurationInjection(@RestResource final Configuration config) {
            // Does nested class without @RestBootstrap inherit from parent?
            // Or does injection fail?
            Assertions.assertNotNull(config, "Nested without @RestBootstrap should inherit from parent");

            System.out.println("Nested-no-annotation config identity: " + System.identityHashCode(config));
            System.out.println("Outer config identity: " + System.identityHashCode(STATIC_CONFIGURATION));

            // Should be the same as outer
            checkInstance(STATIC_CONFIGURATION, config);
        }
    }
}
