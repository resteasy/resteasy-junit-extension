/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.annotations.SelfSignedCert;
import dev.resteasy.junit.extension.annotations.SslCert;
import dev.resteasy.junit.extension.api.SelfSignedCertificate;
import dev.resteasy.junit.extension.extension.resources.RequestInfoResource;

/**
 * Tests {@link SelfSignedCert @SelfSignedCert} combined with {@link RestBootstrap @RestBootstrap}.
 * Verifies the server starts on HTTPS and the injected client is configured with the client SSL context.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(RequestInfoResource.class)
@SelfSignedCert
public class SslBootstrapTest {

    @SslCert
    private SelfSignedCertificate certificate;

    @RestResource
    private Client client;

    @RestResource
    private URI baseUri;

    @Test
    public void serverUsesHttps() {
        Assertions.assertEquals("https", baseUri.getScheme(),
                "Server should be running on HTTPS when @SelfSignedCert is present");
    }

    @Test
    public void injectedClientWorksOverHttps() {
        final String result = client.target(baseUri)
                .path("/info/request-uri")
                .request()
                .get(String.class);
        Assertions.assertEquals("https://localhost:8081/info/request-uri", result);
    }

    @Test
    public void checkClientSslContext(@RestResource @RequestPath("/info/request-uri") final URI uri) throws Exception {
        final HttpClient httpClient = HttpClient.newBuilder()
                .sslContext(certificate.clientSslContext())
                .build();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();
        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("https://localhost:8081/info/request-uri", response.body());
    }

    @Test
    public void checkRestClientBuilder(@RestResource @RequestPath("/info/request-uri") final URI uri) {
        try (Client client = ClientBuilder.newBuilder()
                .keyStore(certificate.clientKeyStore(), SelfSignedCertificate.KEYSTORE_PASSWORD)
                .trustStore(certificate.clientTrustStore())
                .build()) {
            final String result = client.target(uri)
                    .request()
                    .get(String.class);
            Assertions.assertEquals("https://localhost:8081/info/request-uri", result);
        }
    }

    @Test
    public void checkRestClientBuilderSslContext(@RestResource @RequestPath("/info/request-uri") final URI uri) {
        try (Client client = ClientBuilder.newBuilder().sslContext(certificate.clientSslContext()).build()) {
            final String result = client.target(uri)
                    .request()
                    .get(String.class);
            Assertions.assertEquals("https://localhost:8081/info/request-uri", result);
        }
    }

    @Test
    public void injectedWebTargetWorksOverHttps(@RestResource @RequestPath("/info/request-uri") final WebTarget target) {
        final String result = target.request()
                .get(String.class);
        Assertions.assertEquals("https://localhost:8081/info/request-uri", result);
    }

    @Test
    public void certificateIsAvailable() {
        Assertions.assertNotNull(certificate, "Certificate should be injected");
        Assertions.assertNotNull(certificate.serverSslContext(), "Server SSLContext should not be null");
        Assertions.assertNotNull(certificate.clientSslContext(), "Client SSLContext should not be null");
    }
}
