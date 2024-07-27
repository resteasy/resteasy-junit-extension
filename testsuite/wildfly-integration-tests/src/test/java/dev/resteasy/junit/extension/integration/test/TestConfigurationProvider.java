/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.integration.test;

import jakarta.ws.rs.SeBootstrap;

import org.kohsuke.MetaInfServices;

import dev.resteasy.junit.extension.api.ConfigurationProvider;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MetaInfServices
public class TestConfigurationProvider implements ConfigurationProvider {
    public static final int PORT = 8085;

    @Override
    public SeBootstrap.Configuration getConfiguration() {
        return SeBootstrap.Configuration.builder().port(PORT).build();
    }
}
