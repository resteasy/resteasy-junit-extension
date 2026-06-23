/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.api;

import java.util.ServiceLoader;

import jakarta.ws.rs.SeBootstrap.Configuration;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Provides custom {@link jakarta.ws.rs.SeBootstrap.Configuration} for test instances.
 * <p>
 * Implementations of this interface can customize how the {@link jakarta.ws.rs.SeBootstrap SeBootstrap} instance is
 * configured for tests. This allows control over port, host, protocol, root path, and other server configuration
 * options.
 * </p>
 *
 * <h2>Usage</h2>
 * <p>
 * Specify a custom provider on the {@link dev.resteasy.junit.extension.annotations.RestBootstrap @RestBootstrap}
 * annotation:
 * </p>
 *
 * <pre>
 * public class CustomPortProvider implements ConfigurationProvider {
 *     &#64;Override
 *     public Configuration getConfiguration() {
 *         return Configuration.builder()
 *                 .port(9090)
 *                 .build();
 *     }
 * }
 *
 * &#64;RestBootstrap(value = MyApp.class, configFactory = CustomPortProvider.class)
 * public class MyTest {
 *     // SeBootstrap instance will run on port 9090
 * }
 * </pre>
 *
 * <h2>Global Provider via ServiceLoader</h2>
 * <p>
 * Instead of specifying the provider on each test, you can register a global provider via
 * {@link java.util.ServiceLoader}. Create a file
 * {@code META-INF/services/dev.resteasy.junit.extension.api.ConfigurationProvider}
 * containing your implementation's fully-qualified class name.
 * </p>
 * <p>
 * When a global provider is registered and no explicit {@code configFactory} is specified on {@code @RestBootstrap},
 * the global provider will be used automatically for all tests.
 * </p>
 *
 * <h2>Requirements</h2>
 * <ul>
 * <li>Must have a public no-argument constructor</li>
 * <li>The {@link #getConfiguration()} method must return a non-null Configuration</li>
 * <li>Can be registered globally via ServiceLoader or specified per-test via {@code @RestBootstrap(configFactory = ...)}</li>
 * </ul>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @see dev.resteasy.junit.extension.annotations.RestBootstrap#configFactory()
 * @since 1.0.0
 */
public interface ConfigurationProvider {

    /**
     * Provides the configuration for {@linkplain jakarta.ws.rs.SeBootstrap#start(Class, Configuration) starting} a
     * {@link jakarta.ws.rs.SeBootstrap.Instance}.
     * <p>
     * The default implementation checks for a globally registered provider via {@link ServiceLoader}. If found, that
     * provider's configuration is used. Otherwise, returns the default configuration created by
     * {@link Configuration#builder()}.{@link jakarta.ws.rs.SeBootstrap.Configuration.Builder#build() build()}.
     * </p>
     * <p>
     * Implementations should override this method to provide custom configuration values such as port, host, protocol,
     * or root path.
     * </p>
     *
     * @param context the JUnit extension context
     *
     * @return the configuration to use for starting the SeBootstrap instance, must not be {@code null}
     */
    default Configuration getConfiguration(final ExtensionContext context) {
        final ServiceLoader<ConfigurationProvider> loader = ServiceLoader.load(ConfigurationProvider.class);
        if (loader.iterator().hasNext()) {
            return loader.iterator().next().getConfiguration(context);
        }
        final Configuration.Builder builder = Configuration.builder();
        context.getConfigurationParameter("dev.resteasy.junit.extension.protocol").ifPresent(builder::protocol);
        context.getConfigurationParameter("dev.resteasy.junit.extension.host").ifPresent(builder::host);
        context.getConfigurationParameter("dev.resteasy.junit.extension.port", Integer::parseInt).ifPresent(builder::port);
        context.getConfigurationParameter("dev.resteasy.junit.extension.root-path").ifPresent(builder::rootPath);
        return builder.build();
    }
}
