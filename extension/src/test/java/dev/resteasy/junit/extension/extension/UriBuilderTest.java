/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.extension.resources.TestApplication;

/**
 * Tests UriBuilder injection.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(TestApplication.class)
public class UriBuilderTest {

    @RestResource
    private Client client;

    @Test
    public void uriBuilderParameter(@RestResource final UriBuilder builder) {
        Assertions.assertNotNull(builder, "UriBuilder should be injected");
        final URI uri = builder.path("/echo").build();
        Assertions.assertNotNull(uri, "UriBuilder should build a valid URI");

        try (Response response = client.target(uri).request().post(Entity.text("builder test"))) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("builder test", response.readEntity(String.class));
        }
    }

    @Test
    public void multipleUriBuilders(@RestResource final UriBuilder builder1, @RestResource final UriBuilder builder2) {
        Assertions.assertNotNull(builder1, "First UriBuilder should be injected");
        Assertions.assertNotNull(builder2, "Second UriBuilder should be injected");

        // UriBuilders should be different instances (they're mutable)
        Assertions.assertNotSame(builder1, builder2,
                "UriBuilder instances should be different to avoid mutation conflicts");

        // But they should both build the same base URI
        final URI uri1 = builder1.build();
        final URI uri2 = builder2.build();
        Assertions.assertEquals(uri1, uri2, "Both builders should produce the same base URI");
    }

    @Test
    public void uriBuilderMutability(@RestResource final UriBuilder builder) {
        final URI baseUri = builder.build();

        // Mutate the builder
        builder.path("/echo");
        final URI modifiedUri = builder.build();

        Assertions.assertNotEquals(baseUri, modifiedUri, "UriBuilder should be mutable");
        Assertions.assertTrue(modifiedUri.toString().contains("/echo"),
                "Modified URI should contain the path");
    }
}
