/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.api;

import java.util.ServiceLoader;

import jakarta.ws.rs.client.ClientBuilder;

/**
 * A provider for creating the configuration for an injected {@linkplain jakarta.ws.rs.client.Client REST client}. The
 * {@link ClientBuilder} provided by this provider will be used to create the
 * {@linkplain jakarta.ws.rs.client.Client REST client}.
 * <p>
 * By default, this provider attempts to use a {@link java.util.ServiceLoader} to lookup the first provider found. If
 * found, that provider will be used. This can be useful when when you want to use the same provider across all tests
 * without having to define the type on each annotation.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface RestClientBuilderProvider {

    /**
     * A provider which can supply a {@link ClientBuilder} for create the
     * {@linkplain jakarta.ws.rs.client.Client REST client}.
     *
     * @return the {@link ClientBuilder} to use for creating the REST client, cannot be {@code null}
     */
    default ClientBuilder getClientBuilder() {
        final ServiceLoader<RestClientBuilderProvider> loader = ServiceLoader.load(RestClientBuilderProvider.class);
        if (loader.iterator().hasNext()) {
            return loader.iterator().next().getClientBuilder();
        }
        return ClientBuilder.newBuilder();
    }
}
