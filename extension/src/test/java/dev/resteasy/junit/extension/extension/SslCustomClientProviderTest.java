/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestClientConfig;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.annotations.SelfSignedCert;
import dev.resteasy.junit.extension.annotations.SslCert;
import dev.resteasy.junit.extension.api.RestClientBuilderProvider;
import dev.resteasy.junit.extension.api.SelfSignedCertificate;
import dev.resteasy.junit.extension.extension.resources.RequestInfoResource;

/**
 * Tests that a custom {@link RestClientBuilderProvider} receives the {@link SelfSignedCertificate} via
 * {@link SslCert @SslCert} field injection.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@SelfSignedCert
@RestBootstrap(RequestInfoResource.class)
public class SslCustomClientProviderTest {

    public static class CustomSslClientProvider implements RestClientBuilderProvider {
        @SslCert
        private SelfSignedCertificate certificate;

        @Override
        public ClientBuilder getClientBuilder() {
            Assertions.assertNotNull(certificate, "Certificate should be injected into RestClientBuilderProvider");
            return ClientBuilder.newBuilder()
                    .sslContext(certificate.clientSslContext());
        }
    }

    @RestResource
    @RestClientConfig(CustomSslClientProvider.class)
    private Client client;

    @RestResource
    @RequestPath("info/request-uri")
    private URI baseUri;

    @Test
    public void customClientProviderWorksOverHttps() {
        final String result = client.target(baseUri)
                .request()
                .get(String.class);
        Assertions.assertEquals("https://localhost:8081/info/request-uri", result);
    }
}
