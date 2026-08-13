/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import java.net.URI;

import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.client.Client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.annotations.SelfSignedCert;
import dev.resteasy.junit.extension.annotations.SslCert;
import dev.resteasy.junit.extension.api.ConfigurationProvider;
import dev.resteasy.junit.extension.api.SelfSignedCertificate;
import dev.resteasy.junit.extension.extension.resources.RequestInfoResource;

/**
 * Tests that a custom {@link ConfigurationProvider} receives the {@link SelfSignedCertificate} via
 * {@link SslCert @SslCert} field injection.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@SelfSignedCert
@RestBootstrap(value = RequestInfoResource.class, configFactory = SslCustomConfigProviderTest.CustomSslConfigProvider.class)
public class SslCustomConfigProviderTest {

    public static class CustomSslConfigProvider implements ConfigurationProvider {
        @SslCert
        private SelfSignedCertificate certificate;

        @Override
        public SeBootstrap.Configuration getConfiguration(final ExtensionContext context) {
            Assertions.assertNotNull(certificate, "Certificate should be injected into ConfigurationProvider");
            return SeBootstrap.Configuration.builder()
                    .protocol("HTTPS")
                    .sslContext(certificate.serverSslContext())
                    .build();
        }
    }

    @RestResource
    private Client client;

    @RestResource
    @RequestPath("info/request-uri")
    private URI baseUri;

    @Test
    public void serverUsesHttps() {
        Assertions.assertEquals("https", baseUri.getScheme(),
                "Server should be running on HTTPS with custom ConfigurationProvider");
    }

    @Test
    public void clientCanCommunicate() {
        final String result = client.target(baseUri)
                .request()
                .get(String.class);
        Assertions.assertEquals("https://localhost:8081/info/request-uri", result);
    }
}
