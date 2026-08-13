/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.SeBootstrap.Configuration.SSLClientAuthentication;

import org.junit.jupiter.api.extension.ExtensionContext;

import dev.resteasy.junit.extension.api.ConfigurationProvider;
import dev.resteasy.junit.extension.api.SelfSignedCertificate;

/**
 * Default implementation for the configuration provider.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class DefaultConfigurationProvider implements ConfigurationProvider {
    private final SelfSignedCertificate certificate;
    private final SSLClientAuthentication sslClientAuthentication;

    DefaultConfigurationProvider(final SelfSignedCertificate certificate,
            final SSLClientAuthentication sslClientAuthentication) {
        this.certificate = certificate;
        this.sslClientAuthentication = sslClientAuthentication;
    }

    @Override
    public SeBootstrap.Configuration getConfiguration(final ExtensionContext context) {
        final SeBootstrap.Configuration.Builder builder = SeBootstrap.Configuration.builder();
        context.getConfigurationParameter("dev.resteasy.junit.extension.protocol").ifPresent(builder::protocol);
        context.getConfigurationParameter("dev.resteasy.junit.extension.host").ifPresent(builder::host);
        context.getConfigurationParameter("dev.resteasy.junit.extension.port", Integer::parseInt)
                .ifPresent(builder::port);
        context.getConfigurationParameter("dev.resteasy.junit.extension.root-path").ifPresent(builder::rootPath);
        if (certificate != null) {
            builder.sslContext(certificate.serverSslContext());
            builder.protocol("https");
            builder.sslClientAuthentication(sslClientAuthentication);
        }
        return builder.build();
    }
}
