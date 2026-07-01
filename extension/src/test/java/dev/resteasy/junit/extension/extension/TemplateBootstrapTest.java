/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import java.util.Collection;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.extension.resources.EchoResource;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(resources = EchoResource.class)
class TemplateBootstrapTest {

    @RestResource
    private static Client staticClient;

    @RestResource
    private Client client;

    @RestResource
    @RequestPath("echo")
    private WebTarget webTarget;

    @ParameterizedTest
    @ValueSource(strings = { "json", "xml" })
    void parameterized(final String value) {
        try (Response response = webTarget.request().post(Entity.text(value))) {
            Assertions.assertEquals(200, response.getStatus(), () -> String.format("Unexpected response code %s: %s",
                    response.getStatus(), response.readEntity(String.class)));
            Assertions.assertEquals(value, response.readEntity(String.class));
        }
        Assertions.assertSame(staticClient, client, "The staticClient and client should be the same instance.");
    }

    @ParameterizedTest
    @ValueSource(strings = { "json", "xml" })
    void parameterizedWithParameter(final String value, @RestResource final Client pClient) {
        try (Response response = webTarget.request().post(Entity.text(value))) {
            Assertions.assertEquals(200, response.getStatus(), () -> String.format("Unexpected response code %s: %s",
                    response.getStatus(), response.readEntity(String.class)));
            Assertions.assertEquals(value, response.readEntity(String.class));
        }
        Assertions.assertSame(staticClient, client, "The staticClient and client should be the same instance.");
        Assertions.assertSame(staticClient, pClient, "The staticClient and parameter client should be the same instance.");
        Assertions.assertSame(client, pClient, "The client and parameter client should be the same instance.");
    }

    @RepeatedTest(3)
    void repeated(final RepetitionInfo repetitionInfo) {
        final String value = repetitionInfo.getCurrentRepetition() + "-" + repetitionInfo.getTotalRepetitions();
        try (Response response = webTarget.request().post(Entity.text(value))) {
            Assertions.assertEquals(200, response.getStatus(), () -> String.format("Unexpected response code %s: %s",
                    response.getStatus(), response.readEntity(String.class)));
            Assertions.assertEquals(value, response.readEntity(String.class));
        }
        Assertions.assertSame(staticClient, client, "The staticClient and client should be the same instance.");
    }

    @RepeatedTest(3)
    void repeatedWithParameter(final RepetitionInfo repetitionInfo, @RestResource final Client pClient) {
        final String value = repetitionInfo.getCurrentRepetition() + "-" + repetitionInfo.getTotalRepetitions();
        try (Response response = webTarget.request().post(Entity.text(value))) {
            Assertions.assertEquals(200, response.getStatus(), () -> String.format("Unexpected response code %s: %s",
                    response.getStatus(), response.readEntity(String.class)));
            Assertions.assertEquals(value, response.readEntity(String.class));
        }
        Assertions.assertSame(staticClient, client, "The staticClient and client should be the same instance.");
        Assertions.assertSame(staticClient, pClient, "The staticClient and parameter client should be the same instance.");
        Assertions.assertSame(client, pClient, "The client and parameter client should be the same instance.");
    }

    @TestFactory
    Collection<DynamicTest> dynamicTests() {
        return List.of(
                DynamicTest.dynamicTest("post", () -> {
                    final String value = "dynamic";
                    try (Response response = webTarget.request().post(Entity.text(value))) {
                        Assertions.assertEquals(200, response.getStatus(),
                                () -> String.format("Unexpected response code %s: %s",
                                        response.getStatus(), response.readEntity(String.class)));
                        Assertions.assertEquals(value, response.readEntity(String.class));
                    }
                    Assertions.assertSame(staticClient, client, "The staticClient and client should be the same instance.");
                }));
    }

    @TestFactory
    Collection<DynamicTest> dynamicTestsWithParameter(@RestResource final Client pClient) {
        return List.of(
                DynamicTest.dynamicTest("post", () -> {
                    final String value = "dynamic";
                    try (Response response = webTarget.request().post(Entity.text(value))) {
                        Assertions.assertEquals(200, response.getStatus(),
                                () -> String.format("Unexpected response code %s: %s",
                                        response.getStatus(), response.readEntity(String.class)));
                        Assertions.assertEquals(value, response.readEntity(String.class));
                    }
                }),
                DynamicTest.dynamicTest("client", () -> {
                    Assertions.assertSame(staticClient, client, "The staticClient and client should be the same instance.");
                    Assertions.assertSame(staticClient, pClient,
                            "The staticClient and parameter client should be the same instance.");
                    Assertions.assertSame(client, pClient, "The client and parameter client should be the same instance.");
                }));
    }
}
