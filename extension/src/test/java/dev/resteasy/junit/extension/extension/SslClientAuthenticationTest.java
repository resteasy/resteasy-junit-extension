/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import java.net.URI;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.SeBootstrap.Configuration.SSLClientAuthentication;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.annotations.SelfSignedCert;
import dev.resteasy.junit.extension.annotations.SslCert;
import dev.resteasy.junit.extension.api.SelfSignedCertificate;
import dev.resteasy.junit.extension.extension.resources.RequestInfoResource;

/**
 * Tests the {@link RestBootstrap#sslClientAuthentication()} attribute with different values.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class SslClientAuthenticationTest {

    @Nested
    @SelfSignedCert
    @RestBootstrap(value = RequestInfoResource.class, sslClientAuthentication = SSLClientAuthentication.OPTIONAL)
    class OptionalClientAuth {

        @SslCert
        private SelfSignedCertificate certificate;

        @RestResource
        private Client client;

        @RestResource
        @RequestPath("info/request-uri")
        private URI baseUri;

        @Test
        public void fullClientAuthWorks() {
            Assertions.assertEquals("https", baseUri.getScheme());
            final String result = client.target(baseUri)
                    .request()
                    .get(String.class);
            Assertions.assertEquals("https://localhost:8081/info/request-uri", result);
        }

        @Test
        public void trustOnlyClientSucceeds() {
            try (Client trustOnlyClient = ClientBuilder.newBuilder()
                    .trustStore(certificate.clientTrustStore())
                    .build()) {
                final String result = trustOnlyClient.target(baseUri)
                        .request()
                        .get(String.class);
                Assertions.assertEquals("https://localhost:8081/info/request-uri", result);
            }
        }
    }

    @Nested
    @SelfSignedCert
    @RestBootstrap(value = RequestInfoResource.class, sslClientAuthentication = SSLClientAuthentication.MANDATORY)
    class MandatoryClientAuth {

        @SslCert
        private SelfSignedCertificate certificate;

        @RestResource
        private Client client;

        @RestResource
        @RequestPath("info/request-uri")
        private URI baseUri;

        @Test
        public void fullClientAuthWorks() {
            Assertions.assertEquals("https", baseUri.getScheme());
            final String result = client.target(baseUri)
                    .request()
                    .get(String.class);
            Assertions.assertEquals("https://localhost:8081/info/request-uri", result);
        }

        @Test
        public void trustOnlyClientFails() {
            try (Client trustOnlyClient = ClientBuilder.newBuilder()
                    .trustStore(certificate.clientTrustStore())
                    .build()) {
                Assertions.assertThrows(ProcessingException.class, () -> trustOnlyClient.target(baseUri)
                        .request()
                        .get());
            }
        }
    }

    @Nested
    @SelfSignedCert
    @RestBootstrap(value = RequestInfoResource.class, sslClientAuthentication = SSLClientAuthentication.NONE)
    class NoClientAuth {

        @SslCert
        private SelfSignedCertificate certificate;

        @RestResource
        private Client client;

        @RestResource
        @RequestPath("info/request-uri")
        private URI baseUri;

        @Test
        public void fullClientAuthWorks() {
            Assertions.assertEquals("https", baseUri.getScheme());
            final String result = client.target(baseUri)
                    .request()
                    .get(String.class);
            Assertions.assertEquals("https://localhost:8081/info/request-uri", result);
        }

        @Test
        public void trustOnlyClientSucceeds() {
            try (Client trustOnlyClient = ClientBuilder.newBuilder()
                    .trustStore(certificate.clientTrustStore())
                    .build()) {
                final String result = trustOnlyClient.target(baseUri)
                        .request()
                        .get(String.class);
                Assertions.assertEquals("https://localhost:8081/info/request-uri", result);
            }
        }
    }
}
