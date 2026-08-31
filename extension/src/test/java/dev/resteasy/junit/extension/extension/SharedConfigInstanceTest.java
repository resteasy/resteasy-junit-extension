/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import jakarta.ws.rs.SeBootstrap.Configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.extension.resources.TestApplication;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RestBootstrap(application = TestApplication.class)
public class SharedConfigInstanceTest {
    @RestResource
    private static Configuration STATIC_CONFIGURATION;

    private static Configuration beforeAllConfiguration;

    @RestResource
    private Configuration instanceConfiguration;
    private final Configuration constructorConfiguration;

    public SharedConfigInstanceTest(@RestResource final Configuration constructorConfiguration) {
        this.constructorConfiguration = constructorConfiguration;
    }

    @BeforeAll
    public static void beforeAll(@RestResource final Configuration configuration) {
        beforeAllConfiguration = configuration;
    }

    @Test
    public void configurationIdentity(@RestResource final Configuration parameterConfiguration) {
        Assertions.assertNotNull(parameterConfiguration, "Parameter configuration should be injected");
        // Every injection point resolves to the same Configuration instance
        checkInstance(beforeAllConfiguration, parameterConfiguration);
        checkInstance(STATIC_CONFIGURATION, parameterConfiguration);
        checkInstance(instanceConfiguration, parameterConfiguration);
        checkInstance(constructorConfiguration, parameterConfiguration);
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
    @RestBootstrap(application = TestApplication.class, configFactory = SecondInstanceConfigurationProvider.class)
    class NestedWithBootstrap {
        @RestResource
        private Configuration nestedConfiguration;

        @Test
        public void nestedConfigurationIdentity(@RestResource final Configuration nestedParamConfiguration) {
            Assertions.assertNotNull(nestedConfiguration, "Nested configuration should be injected");
            Assertions.assertNotNull(nestedParamConfiguration, "Nested parameter configuration should be injected");

            // Within nested class, field and parameter injection resolve to the same instance
            checkInstance(nestedConfiguration, nestedParamConfiguration);

            // A nested class with its own @RestBootstrap gets its OWN Configuration, distinct from the outer class.
            // SecondInstanceConfigurationProvider binds it to port 9081.
            Assertions.assertNotSame(STATIC_CONFIGURATION, nestedConfiguration,
                    "Nested class with @RestBootstrap should have a different Configuration than the outer class");
            Assertions.assertEquals(9081, nestedConfiguration.port(),
                    "Nested configuration should use the port from SecondInstanceConfigurationProvider");
            Assertions.assertNotEquals(STATIC_CONFIGURATION.port(), nestedConfiguration.port(),
                    "Nested configuration should use a different port than the outer configuration");
        }
    }

    @Nested
    class NestedWithoutBootstrap {
        @Test
        public void attemptConfigurationInjection(@RestResource final Configuration config) {
            // A nested class without its own @RestBootstrap inherits the outer class's Configuration.
            Assertions.assertNotNull(config, "Nested without @RestBootstrap should inherit from parent");
            checkInstance(STATIC_CONFIGURATION, config);
        }
    }
}
