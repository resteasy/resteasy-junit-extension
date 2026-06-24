/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.integration.test;

import jakarta.ws.rs.SeBootstrap;

import org.junit.jupiter.api.extension.ExtensionContext;

import dev.resteasy.junit.extension.api.ConfigurationProvider;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class TestConfigurationProvider implements ConfigurationProvider {
    public static final int PORT = 8085;

    @Override
    public SeBootstrap.Configuration getConfiguration(final ExtensionContext context) {
        return SeBootstrap.Configuration.builder().port(PORT).build();
    }
}
