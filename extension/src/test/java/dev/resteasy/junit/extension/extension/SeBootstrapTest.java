/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import jakarta.inject.Inject;
import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
            return SeBootstrap.Configuration.builder().port(8095).build();
        }
    }

    @Inject
    private Client client;

    @Inject
    private UriBuilder uriBuilder;

    @Test
    public void portOverrideCheck() {
        Assertions.assertEquals(8095, uriBuilder.build().getPort());
    }

    @Test
    public void invokeResource(final UriBuilder builder) {
        try (Client client = ClientBuilder.newClient()) {
            final String result = client.target(builder.path("/test/echo"))
                    .request()
                    .post(Entity.text("Hello"), String.class);
            Assertions.assertEquals("Hello", result);
        }
    }

    @Test
    public void invokeResourceOnInjectedClient() {
        final String result = client.target(uriBuilder.path("/test/echo"))
                .request()
                .post(Entity.text("Hello"), String.class);
        Assertions.assertEquals("Hello", result);
    }

}
