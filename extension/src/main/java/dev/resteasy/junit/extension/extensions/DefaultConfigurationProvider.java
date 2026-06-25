/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import jakarta.ws.rs.SeBootstrap;

import org.junit.jupiter.api.extension.ExtensionContext;

import dev.resteasy.junit.extension.api.ConfigurationProvider;

/**
 * Default implementation for the configuration provider.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class DefaultConfigurationProvider implements ConfigurationProvider {
    @Override
    public SeBootstrap.Configuration getConfiguration(final ExtensionContext context) {
        final SeBootstrap.Configuration.Builder builder = SeBootstrap.Configuration.builder();
        context.getConfigurationParameter("dev.resteasy.junit.extension.protocol").ifPresent(builder::protocol);
        context.getConfigurationParameter("dev.resteasy.junit.extension.host").ifPresent(builder::host);
        context.getConfigurationParameter("dev.resteasy.junit.extension.port", Integer::parseInt)
                .ifPresent(builder::port);
        context.getConfigurationParameter("dev.resteasy.junit.extension.root-path").ifPresent(builder::rootPath);
        return builder.build();
    }
}
