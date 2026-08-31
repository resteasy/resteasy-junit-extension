/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.annotation.Priority;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestClientConfig;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.api.RestClientBuilderProvider;
import dev.resteasy.junit.extension.extension.resources.EchoResource;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(EchoResource.class)
class RestClientConfigTest {

    @RestResource
    @RestClientConfig(TestClientBuilderProvider.class)
    private Client providerOnlyClient;

    @RestResource
    @RestClientConfig(value = TestClientBuilderProvider.class, providers = TestFilter.class)
    private Client providerAndFilterClient;

    @RestResource
    @RestClientConfig(providers = TestFilter.class)
    private Client filterOnlyClient;

    @RestResource
    @RestClientConfig(providers = TestFilter.class)
    private Client filterOnlyClient2;

    @Test
    void providerOnly(final UriBuilder uriBuilder) {
        try (
                Response response = providerOnlyClient.target(uriBuilder.path("/echo")).request()
                        .post(Entity.text("entity"))) {
            Assertions.assertEquals(200, response.getStatus(), () -> String.format("Expected a 200 response, but got %d: %s",
                    response.getStatus(), response.readEntity(String.class)));
            Assertions.assertEquals("Builder-Request entity Builder-Response", response.readEntity(String.class));
        }
    }

    @Test
    void filterOnly(final UriBuilder uriBuilder) {
        try (
                Response response = filterOnlyClient.target(uriBuilder.path("/echo")).request()
                        .post(Entity.text("entity"))) {
            Assertions.assertEquals(200, response.getStatus(), () -> String.format("Expected a 200 response, but got %d: %s",
                    response.getStatus(), response.readEntity(String.class)));
            Assertions.assertEquals("Request entity Response", response.readEntity(String.class));
        }
    }

    @Test
    void sameInstance() {
        Assertions.assertSame(filterOnlyClient, filterOnlyClient2);
    }

    @Test
    void providerAndFilter(final UriBuilder uriBuilder) {
        try (
                Response response = providerAndFilterClient.target(uriBuilder.path("/echo")).request()
                        .post(Entity.text("entity"))) {
            Assertions.assertEquals(200, response.getStatus(), () -> String.format("Expected a 200 response, but got %d: %s",
                    response.getStatus(), response.readEntity(String.class)));
            // Request filters run in ascending @Priority order (TestBuilderFilter then TestFilter) and prepend;
            // response filters run in descending order (TestFilter then TestBuilderFilter) and append.
            Assertions.assertEquals("Request Builder-Request entity Response Builder-Response",
                    response.readEntity(String.class));
        }
    }

    @Priority(200)
    public static class TestFilter implements ClientRequestFilter, ClientResponseFilter {
        @Override
        public void filter(final ClientRequestContext requestContext) {
            final Object entity = requestContext.getEntity();
            if (entity != null) {
                requestContext.setEntity("Request " + entity);
            }
        }

        @Override
        public void filter(final ClientRequestContext requestContext, final ClientResponseContext responseContext)
                throws IOException {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                responseContext.getEntityStream().transferTo(baos);
                baos.write(" Response".getBytes(StandardCharsets.UTF_8));
                responseContext.setEntityStream(new ByteArrayInputStream(baos.toByteArray()));
            }
        }
    }

    @Priority(100)
    public static class TestBuilderFilter implements ClientRequestFilter, ClientResponseFilter {
        @Override
        public void filter(final ClientRequestContext requestContext) {
            final Object entity = requestContext.getEntity();
            if (entity != null) {
                requestContext.setEntity("Builder-Request " + entity);
            }
        }

        @Override
        public void filter(final ClientRequestContext requestContext, final ClientResponseContext responseContext)
                throws IOException {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                responseContext.getEntityStream().transferTo(baos);
                baos.write(" Builder-Response".getBytes(StandardCharsets.UTF_8));
                responseContext.setEntityStream(new ByteArrayInputStream(baos.toByteArray()));
            }
        }
    }

    public static class TestClientBuilderProvider implements RestClientBuilderProvider {

        @Override
        public ClientBuilder getClientBuilder() {
            return ClientBuilder.newBuilder()
                    .register(TestBuilderFilter.class);
        }
    }
}
