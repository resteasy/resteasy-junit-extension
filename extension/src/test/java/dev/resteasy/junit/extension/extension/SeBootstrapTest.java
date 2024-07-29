/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.api.ConfigurationProvider;
import dev.resteasy.junit.extension.extension.resources.TestApplication;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RestBootstrap(value = TestApplication.class, configFactory = SeBootstrapTest.PortChangeConfigurationFactory.class)
public class SeBootstrapTest {
    public static class PortChangeConfigurationFactory implements ConfigurationProvider {
        @Override
        public SeBootstrap.Configuration getConfiguration() {
            return SeBootstrap.Configuration.builder().port(8095).rootPath("/test").build();
        }
    }

    @Inject
    private static Client CLIENT;

    @Inject
    private static URI STATIC_URI;

    @Inject
    @RequestPath("/echo")
    private static WebTarget TARGET;

    @Inject
    private Client client;

    @Inject
    private URI uri;

    @Inject
    @RequestPath("/echo")
    private WebTarget target;

    @Test
    public void portOverrideCheck() {
        Assertions.assertEquals(8095, uri.getPort());
    }

    @Test
    public void staticPortOverrideCheck() {
        Assertions.assertEquals(8095, STATIC_URI.getPort());
    }

    @Test
    public void invokeResource(final URI uri) {
        try (Client client = ClientBuilder.newClient()) {
            final String result = client.target(UriBuilder.fromUri(uri).path("/echo"))
                    .request()
                    .post(Entity.text("Hello"), String.class);
            Assertions.assertEquals("Hello", result);
        }
    }

    @Test
    public void invokeResource(final UriBuilder builder) {
        try (Client client = ClientBuilder.newClient()) {
            final String result = client.target(builder.path("/echo"))
                    .request()
                    .post(Entity.text("Hello"), String.class);
            Assertions.assertEquals("Hello", result);
        }
    }

    @Test
    public void invokeResourceOnInjectedClient() {
        final String result = client.target(UriBuilder.fromUri(uri).path("/echo"))
                .request()
                .post(Entity.text("Hello"), String.class);
        Assertions.assertEquals("Hello", result);
    }

    @Test
    public void staticInvokeResourceOnInjectedClient() {
        final String result = CLIENT.target(UriBuilder.fromUri(uri).path("/echo"))
                .request()
                .post(Entity.text("Hello"), String.class);
        Assertions.assertEquals("Hello", result);
    }

    @Test
    public void invokeWebTarget(@RequestPath("/echo") final WebTarget target) {
        final String result = target.request()
                .post(Entity.text("Hello"), String.class);
        Assertions.assertEquals("Hello", result);
    }

    @Test
    public void invokeOnInjectedWebTarget() {
        final String result = target.request()
                .post(Entity.text("Hello"), String.class);
        Assertions.assertEquals("Hello", result);
    }

    @Test
    public void staticInvokeOnInjectedWebTarget() {
        final String result = TARGET.request()
                .post(Entity.text("Hello"), String.class);
        Assertions.assertEquals("Hello", result);
    }
}
