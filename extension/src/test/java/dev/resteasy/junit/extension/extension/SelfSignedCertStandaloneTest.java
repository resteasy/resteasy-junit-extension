/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import java.nio.file.Files;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.SelfSignedCert;
import dev.resteasy.junit.extension.annotations.SslCert;
import dev.resteasy.junit.extension.api.SelfSignedCertificate;

/**
 * Tests {@link SelfSignedCert @SelfSignedCert} standalone without {@link
 * dev.resteasy.junit.extension.annotations.RestBootstrap @RestBootstrap}.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@SelfSignedCert
public class SelfSignedCertStandaloneTest {

    @SslCert
    private static SelfSignedCertificate STATIC_CERT;

    @SslCert
    private SelfSignedCertificate instanceCert;

    @Test
    public void staticFieldInjected() {
        Assertions.assertNotNull(STATIC_CERT, "Static @SslCert field should be injected");
    }

    @Test
    public void instanceFieldInjected() {
        Assertions.assertNotNull(instanceCert, "Instance @SslCert field should be injected");
    }

    @Test
    public void parameterInjected(final SelfSignedCertificate cert) {
        Assertions.assertNotNull(cert, "Method parameter should be injected by type");
    }

    @Test
    public void sameInstance(final SelfSignedCertificate cert) {
        Assertions.assertSame(STATIC_CERT, instanceCert,
                "Static and instance fields should be the same instance");
        Assertions.assertSame(STATIC_CERT, cert,
                "Static field and parameter should be the same instance");
    }

    @Test
    public void serverSslContext() {
        final SSLContext ctx = STATIC_CERT.serverSslContext();
        Assertions.assertNotNull(ctx, "Server SSLContext should not be null");
        Assertions.assertEquals("TLS", ctx.getProtocol(), "Server SSLContext protocol should be TLS");
    }

    @Test
    public void clientSslContext() {
        final SSLContext ctx = STATIC_CERT.clientSslContext();
        Assertions.assertNotNull(ctx, "Client SSLContext should not be null");
        Assertions.assertEquals("TLS", ctx.getProtocol(), "Client SSLContext protocol should be TLS");
    }

    @Test
    public void keystoreObjectsExist() {
        Assertions.assertNotNull(STATIC_CERT.serverKeyStore(), "Server keystore should not be null");
        Assertions.assertNotNull(STATIC_CERT.serverTrustStore(), "Server truststore should not be null");
        Assertions.assertNotNull(STATIC_CERT.clientKeyStore(), "Client keystore should not be null");
        Assertions.assertNotNull(STATIC_CERT.clientTrustStore(), "Client truststore should not be null");
    }

    @Test
    public void keystoreFilesExist() {
        Assertions.assertTrue(Files.exists(STATIC_CERT.serverKeyStorePath()),
                "Server keystore file should exist");
        Assertions.assertTrue(Files.exists(STATIC_CERT.serverTrustStorePath()),
                "Server truststore file should exist");
        Assertions.assertTrue(Files.exists(STATIC_CERT.clientKeyStorePath()),
                "Client keystore file should exist");
        Assertions.assertTrue(Files.exists(STATIC_CERT.clientTrustStorePath()),
                "Client truststore file should exist");
    }

    @Test
    public void keystorePassword() {
        Assertions.assertNotNull(STATIC_CERT.keyStorePassword(), "Keystore password should not be null");
        Assertions.assertFalse(STATIC_CERT.keyStorePassword().isEmpty(), "Keystore password should not be empty");
    }

    @Test
    public void keystoreType() {
        Assertions.assertEquals("PKCS12", STATIC_CERT.keyStoreType(), "Keystore type should be PKCS12");
    }

    @Nested
    class NestedInherited {

        @SslCert
        private SelfSignedCertificate nestedCert;

        @Test
        public void nestedFieldInjected() {
            Assertions.assertNotNull(nestedCert, "Nested @SslCert field should be injected");
        }

        @Test
        public void nestedSharesParentCert() {
            Assertions.assertSame(STATIC_CERT, nestedCert,
                    "Nested class should share parent's certificate (annotation is @Inherited)");
        }

        @Test
        public void nestedParameterInjected(final SelfSignedCertificate cert) {
            Assertions.assertNotNull(cert, "Nested method parameter should be injected by type");
            Assertions.assertSame(cert, nestedCert,
                    "Nested parameter should be the same instance as the nestedCert");
            Assertions.assertSame(STATIC_CERT, cert,
                    "Nested parameter should be the same instance as parent's static field");
        }
    }
}
