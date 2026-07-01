/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import jakarta.ws.rs.client.Client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.extension.resources.TestApplication;

/**
 * Tests Client injection and instance identity across all injection points.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(application = TestApplication.class)
public class ClientInstanceTest {

    @RestResource
    private static Client STATIC_CLIENT;

    private static Client beforeAllClient;

    @RestResource
    private Client instanceClient;

    private Client beforeEachClient;

    private final Client constructorClient;

    public ClientInstanceTest(@RestResource final Client constructorClient) {
        this.constructorClient = constructorClient;
    }

    @BeforeAll
    public static void captureBeforeAll(@RestResource final Client client) {
        Assertions.assertNotNull(STATIC_CLIENT, "@BeforeAll client should not be null");
        Assertions.assertNotNull(client, "@BeforeAll client should not be null");
        Assertions.assertSame(STATIC_CLIENT, client, "@BeforeAll client should be the same instance");
        beforeAllClient = client;
    }

    @BeforeEach
    public void captureBeforeEach(@RestResource final Client client) {
        Assertions.assertNotNull(STATIC_CLIENT, "@BeforeEach client should not be null");
        Assertions.assertNotNull(client, "@BeforeEach client should not be null");
        Assertions.assertSame(STATIC_CLIENT, client, "@BeforeEach client should be the same instance");
        beforeEachClient = client;
    }

    @AfterEach
    public void afterEach(@RestResource final Client client) {
        Assertions.assertNotNull(STATIC_CLIENT, "@AfterEach client should not be null");
        Assertions.assertNotNull(client, "@AfterEach client should not be null");
        Assertions.assertSame(STATIC_CLIENT, client, "@AfterEach client should be the same instance");
    }

    @AfterAll
    public static void afterAll(@RestResource final Client client) {
        Assertions.assertNotNull(STATIC_CLIENT, "@AfterAll client should not be null");
        Assertions.assertNotNull(client, "@AfterAll client should not be null");
        Assertions.assertSame(STATIC_CLIENT, client, "@AfterAll client should be the same instance");
    }

    @Test
    public void clientInstanceIdentity(@RestResource final Client parameterClient) {
        Assertions.assertNotNull(STATIC_CLIENT, "Static client should be injected");
        Assertions.assertNotNull(beforeAllClient, "@BeforeAll client should be injected");
        Assertions.assertNotNull(instanceClient, "Instance client should be injected");
        Assertions.assertNotNull(constructorClient, "Constructor client should be injected");
        Assertions.assertNotNull(beforeEachClient, "@BeforeEach client should be injected");
        Assertions.assertNotNull(parameterClient, "Parameter client should be injected");

        Assertions.assertSame(STATIC_CLIENT, beforeAllClient,
                "Static and @BeforeAll clients should be the same instance");
        Assertions.assertSame(STATIC_CLIENT, instanceClient,
                "Static and instance clients should be the same instance");
        Assertions.assertSame(STATIC_CLIENT, constructorClient,
                "Static and constructor clients should be the same instance");
        Assertions.assertSame(STATIC_CLIENT, beforeEachClient,
                "Static and @BeforeEach clients should be the same instance");
        Assertions.assertSame(STATIC_CLIENT, parameterClient,
                "Static and parameter clients should be the same instance");
    }

    @Test
    public void verifyLifecycleMethodInjection() {
        Assertions.assertNotNull(beforeEachClient, "@BeforeEach should have captured a client");
        Assertions.assertSame(STATIC_CLIENT, beforeEachClient,
                "@BeforeEach client should match static client");
    }

    @Test
    public void multipleParameterClients(@RestResource final Client client1, @RestResource final Client client2) {
        Assertions.assertNotNull(client1, "First client should be injected");
        Assertions.assertNotNull(client2, "Second client should be injected");

        // Multiple parameter clients should be the same instance
        Assertions.assertSame(client1, client2, "Parameter clients should be the same instance");
    }

    @Nested
    @RestBootstrap(application = TestApplication.class, configFactory = SecondInstanceConfigurationProvider.class)
    @Disabled("Disable until we can upgrade to RESTEasy 6.2.17.Final")
    class NestedWithBootstrap {
        @RestResource
        private Client nestedClient;

        private Client nestedBeforeEachClient;

        @BeforeEach
        public void nestedBeforeEach(@RestResource final Client client) {
            Assertions.assertNotNull(nestedClient, "Nested @BeforeEach client should not be null");
            Assertions.assertNotNull(client, "Nested @BeforeEach client should not be null");
            Assertions.assertSame(nestedClient, client, "Nested @BeforeEach client should be the same instance");
            nestedBeforeEachClient = client;
        }

        @AfterEach
        public void nestedAfterEach(@RestResource final Client client) {
            Assertions.assertNotNull(nestedClient, "Nested @AfterEach client should not be null");
            Assertions.assertNotNull(client, "Nested @AfterEach client should not be null");
            Assertions.assertSame(nestedClient, client, "Nested @AfterEach client should be the same instance");
        }

        @Test
        public void nestedClientIdentity(@RestResource final Client nestedParamClient) {
            Assertions.assertNotNull(nestedClient, "Nested instance client should be injected");
            Assertions.assertNotNull(nestedBeforeEachClient, "Nested @BeforeEach client should be injected");
            Assertions.assertNotNull(nestedParamClient, "Nested parameter client should be injected");

            // Within nested class, all clients should be the same
            Assertions.assertSame(nestedClient, nestedBeforeEachClient,
                    "Nested instance and @BeforeEach clients should be the same");
            Assertions.assertSame(nestedClient, nestedParamClient,
                    "Nested instance and parameter clients should be the same");

            // Nested class with @RestBootstrap should have its OWN client (different from outer)
            Assertions.assertNotSame(STATIC_CLIENT, nestedClient,
                    "Nested class with @RestBootstrap should have different Client than outer class");
        }
    }

    @Nested
    class NestedWithoutBootstrap {
        private Client nestedBeforeEachClient;

        @BeforeEach
        public void nestedBeforeEach(@RestResource final Client client) {
            Assertions.assertNotNull(client, "Nested @BeforeEach client should not be null");
            Assertions.assertSame(STATIC_CLIENT, client, "Nested @BeforeEach should inherit outer Client");
            nestedBeforeEachClient = client;
        }

        @AfterEach
        public void nestedAfterEach(@RestResource final Client client) {
            Assertions.assertNotNull(client, "Nested @AfterEach client should not be null");
            Assertions.assertSame(STATIC_CLIENT, client, "Nested @AfterEach should inherit outer Client");
        }

        @Test
        public void attemptClientInjection(@RestResource final Client client) {
            Assertions.assertNotNull(client, "Nested without @RestBootstrap should inherit from parent");
            Assertions.assertNotNull(nestedBeforeEachClient, "Nested @BeforeEach should have captured client");

            Assertions.assertSame(STATIC_CLIENT, client,
                    "Nested class without @RestBootstrap should share outer class Client");
            Assertions.assertSame(STATIC_CLIENT, nestedBeforeEachClient,
                    "Nested @BeforeEach should share outer class Client");
        }
    }
}
