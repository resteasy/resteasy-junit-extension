/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import java.net.URI;

import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.extension.resources.TestApplication;

/**
 * Tests URI injection variations.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(TestApplication.class)
public class UriInjectionTest {

    @RestResource
    private static URI STATIC_URI;

    @RestResource
    private URI instanceUri;

    @RestResource
    private Client client;

    @Test
    public void uriInjection() {
        Assertions.assertNotNull(STATIC_URI, "Static URI should be injected");
        Assertions.assertNotNull(instanceUri, "Instance URI should be injected");
        Assertions.assertEquals(STATIC_URI, instanceUri,
                "Static and instance URIs should be the same");
    }

    @Test
    public void parameterUriInjection(@RestResource final URI paramUri) {
        Assertions.assertNotNull(paramUri, "Parameter URI should be injected");
        Assertions.assertEquals(instanceUri, paramUri,
                "Instance and parameter URIs should be the same");
        Assertions.assertEquals(STATIC_URI, paramUri,
                "Static and parameter URIs should be the same");
    }

    @Test
    public void uriMatchesConfiguration(@RestResource final URI uri, @RestResource final SeBootstrap.Configuration config) {
        Assertions.assertNotNull(uri, "URI should be injected");
        Assertions.assertNotNull(config, "Configuration should be injected");

        Assertions.assertEquals(config.baseUri(), uri,
                "Injected URI should match configuration base URI");
    }

    @Test
    public void uriUsableForRequests(@RestResource final URI baseUri) {
        try (Response response = client.target(baseUri).path("/echo").request()
                .post(Entity.text("uri test"))) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("uri test", response.readEntity(String.class));
        }
    }

    @Test
    public void uriWithRequestPath(@RestResource @RequestPath("/echo") final URI echoUri) {
        Assertions.assertNotNull(echoUri, "URI with @RequestPath should be injected");

        // Should point to the /echo path, not the base URI
        Assertions.assertTrue(echoUri.toString().endsWith("/echo"),
                "URI with @RequestPath should point to the specified path");

        // Should be usable for requests
        try (Response response = client.target(echoUri).request()
                .post(Entity.text("path test"))) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("path test", response.readEntity(String.class));
        }
    }

    @Test
    public void uriWithLeadingSlashPath(@RestResource @RequestPath("/echo") final URI uri1,
            @RestResource @RequestPath("echo") final URI uri2) {
        Assertions.assertNotNull(uri1, "URI with leading slash should be injected");
        Assertions.assertNotNull(uri2, "URI without leading slash should be injected");

        // Both should point to the same path regardless of leading slash
        Assertions.assertTrue(uri1.toString().contains("/echo"),
                "URI should contain /echo path");
        Assertions.assertTrue(uri2.toString().contains("/echo"),
                "URI should contain /echo path");
    }

    @Test
    public void uriWithNestedPath(@RestResource @RequestPath("/api/v1/echo") final URI nestedUri) {
        Assertions.assertNotNull(nestedUri, "URI with nested path should be injected");

        // Should handle nested paths
        Assertions.assertTrue(nestedUri.toString().contains("/api/v1/echo"),
                "URI should contain nested path");
    }
}
