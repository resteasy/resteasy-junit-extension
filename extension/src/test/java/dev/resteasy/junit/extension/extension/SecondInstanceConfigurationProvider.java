/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import jakarta.ws.rs.SeBootstrap;

import org.junit.jupiter.api.extension.ExtensionContext;

import dev.resteasy.junit.extension.api.ConfigurationProvider;

/**
 * Configuration provider for port 9081 (used by nested test classes).
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class SecondInstanceConfigurationProvider implements ConfigurationProvider {
    @Override
    public SeBootstrap.Configuration getConfiguration(final ExtensionContext context) {
        return SeBootstrap.Configuration.builder().port(9081).build();
    }
}
