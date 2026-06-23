/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.api;

import java.util.ServiceLoader;

import jakarta.ws.rs.client.ClientBuilder;

/**
 * Provides custom {@link jakarta.ws.rs.client.ClientBuilder} configuration for creating REST clients.
 * <p>
 * Implementations of this interface can customize how {@link jakarta.ws.rs.client.Client Client} instances are created
 * for tests. This allows configuration of timeouts, SSL settings, custom providers, filters, and other client options.
 * </p>
 *
 * <h2>Usage</h2>
 * <p>
 * Specify a custom provider using the {@link dev.resteasy.junit.extension.annotations.RestClientConfig @RestClientConfig}
 * qualifier on injected Client or WebTarget fields/parameters:
 * </p>
 *
 * <pre>
 * public class CustomTimeoutProvider implements RestClientBuilderProvider {
 *     &#64;Override
 *     public ClientBuilder getClientBuilder() {
 *         return ClientBuilder.newBuilder()
 *                 .connectTimeout(5, TimeUnit.SECONDS)
 *                 .readTimeout(30, TimeUnit.SECONDS);
 *     }
 * }
 *
 * &#64;RestBootstrap(MyApp.class)
 * public class MyTest {
 *     &#64;RestResource
 *     &#64;RestClientConfig(CustomTimeoutProvider.class)
 *     private Client client; // Will use custom timeouts
 *
 *     &#64;RestResource
 *     private Client defaultClient; // Will use default configuration
 * }
 * </pre>
 *
 * <h2>Global Provider via ServiceLoader</h2>
 * <p>
 * Instead of specifying the provider on each injection point, you can register a global provider via
 * {@link java.util.ServiceLoader}. Create a file
 * {@code META-INF/services/dev.resteasy.junit.extension.api.RestClientBuilderProvider}
 * containing your implementation class name.
 * </p>
 * <p>
 * When a global provider is registered and no explicit {@code @RestClientConfig} is specified, the global provider will
 * be used automatically for all injected Client instances.
 * </p>
 *
 * <h2>Requirements</h2>
 * <ul>
 * <li>Must have a public no-argument constructor</li>
 * <li>The {@link #getClientBuilder()} method must return a non-null ClientBuilder</li>
 * <li>Can be registered globally via ServiceLoader or specified per-injection via {@code @RestClientConfig}</li>
 * </ul>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @see dev.resteasy.junit.extension.annotations.RestClientConfig
 * @since 1.0.0
 */
public interface RestClientBuilderProvider {

    /**
     * Provides a {@link ClientBuilder} for creating {@linkplain jakarta.ws.rs.client.Client REST client} instances.
     * <p>
     * The default implementation checks for a globally registered provider via {@link ServiceLoader}. If found, that
     * provider's ClientBuilder is used. Otherwise, returns the default builder created by
     * {@link ClientBuilder#newBuilder()}.
     * </p>
     * <p>
     * Implementations should override this method to provide custom client configuration such as timeouts, SSL settings,
     * providers, or interceptors.
     * </p>
     *
     * @return the {@link ClientBuilder} to use for creating REST client instances, must not be {@code null}
     */
    default ClientBuilder getClientBuilder() {
        final ServiceLoader<RestClientBuilderProvider> loader = ServiceLoader.load(RestClientBuilderProvider.class);
        if (loader.iterator().hasNext()) {
            return loader.iterator().next().getClientBuilder();
        }
        return ClientBuilder.newBuilder();
    }
}
