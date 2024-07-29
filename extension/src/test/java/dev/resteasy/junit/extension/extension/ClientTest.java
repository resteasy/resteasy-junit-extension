/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import jakarta.inject.Inject;
import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.extension.resources.TestApplication;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RestBootstrap(TestApplication.class)
public class ClientTest {

    @Inject
    private static SeBootstrap.Configuration CONFIGURATION;

    @Inject
    private static Client STATIC_CLIENT;

    @Inject
    private Client instanceClient;

    private final Client constructorClient;

    public ClientTest(final Client constructorClient) {
        this.constructorClient = constructorClient;
    }

    @BeforeAll
    public static void checkBeforeAll(final Client client) {
        // The client should be available and ready to use
        assertClientAvailable(client);
    }

    @AfterAll
    public static void checkAfterAll(final Client client) {
        // The client should be available and ready to use
        assertClientAvailable(client);
    }

    @Test
    public void checkConstructorInstance() {
        assertClientAvailable(constructorClient);
    }

    @Test
    public void checkParameter(final Client client) {
        assertClientAvailable(client);
    }

    @Test
    public void checkStaticInjected() {
        assertClientAvailable(STATIC_CLIENT);
    }

    @Test
    public void checkInjected() {
        assertClientAvailable(instanceClient);
    }

    private static void assertClientAvailable(final Client client) {
        var result = client.target(CONFIGURATION.baseUriBuilder().path("/echo")).request()
                .post(Entity.text("Hello1"));
        Assertions.assertEquals(200, result.getStatus(), () -> String.format("Expected a 200 response, but got %d: %s",
                result.getStatus(), result.readEntity(String.class)));
        Assertions.assertEquals("Hello1", result.readEntity(String.class));
    }
}
