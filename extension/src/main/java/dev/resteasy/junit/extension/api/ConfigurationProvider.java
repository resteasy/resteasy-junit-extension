/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.api;

import java.util.ServiceLoader;

import jakarta.ws.rs.SeBootstrap.Configuration;

/**
 * A provider for creating the {@link jakarta.ws.rs.SeBootstrap.Configuration}. The provider must have a public no-arg
 * constructor.
 * <p>
 * By default , this provider attempts to use a {@link java.util.ServiceLoader} to lookup the first provider found. If
 * found that provider will be used. This can be useful when when you want to use the same provider across all tests
 * without having to define the type on each annotation.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface ConfigurationProvider {

    /**
     * The configuration for {@linkplain jakarta.ws.rs.SeBootstrap#start(Class, Configuration) booting} an
     * {@link jakarta.ws.rs.SeBootstrap.Instance}.
     *
     * @return the configuration to use, cannot be {@code null}
     */
    default Configuration getConfiguration() {
        final ServiceLoader<ConfigurationProvider> loader = ServiceLoader.load(ConfigurationProvider.class);
        if (loader.iterator().hasNext()) {
            return loader.iterator().next().getConfiguration();
        }
        return Configuration.builder().build();
    }
}
