/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.api;

import java.nio.file.Path;
import java.security.KeyStore;
import java.util.UUID;

import javax.net.ssl.SSLContext;

/**
 * Provides access to self-signed certificate artifacts generated for SSL/TLS testing.
 * <p>
 * When a test class is annotated with
 * {@link dev.resteasy.junit.extension.annotations.SelfSignedCert @SelfSignedCert}, the extension generates
 * self-signed certificates for both server and client. This type can be injected into test fields annotated with
 * {@link dev.resteasy.junit.extension.annotations.SslCert @SslCert} or into test method parameters by type.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * &#64;SelfSignedCert
 * &#64;RestBootstrap(MyResource.class)
 * public class SslTest {
 *
 *     &#64;SslCert
 *     private SelfSignedCertificate certificate;
 *
 *     &#64;Test
 *     public void testSsl() {
 *         SSLContext clientCtx = certificate.clientSslContext();
 *         // use the client SSL context to configure a non-Jakarta REST client
 *     }
 * }
 * </pre>
 *
 * <p>
 * When combined with {@link dev.resteasy.junit.extension.annotations.RestBootstrap @RestBootstrap}, the server is
 * automatically started with HTTPS and injected {@link jakarta.ws.rs.client.Client Client} instances are configured
 * with the client {@link SSLContext}. In that case, direct access to this type is often not needed.
 * </p>
 * <p>
 * This type is primarily useful when configuring non-Jakarta REST HTTP clients, when direct access to keystore files is
 * required, or when using {@link dev.resteasy.junit.extension.annotations.SelfSignedCert @SelfSignedCert} standalone
 * without {@link dev.resteasy.junit.extension.annotations.RestBootstrap @RestBootstrap}.
 * </p>
 * <p>
 * For custom SSL configuration (e.g., using your own certificates), use a
 * {@link ConfigurationProvider} for the server and a {@link RestClientBuilderProvider} for the client instead.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @see dev.resteasy.junit.extension.annotations.SelfSignedCert
 * @see dev.resteasy.junit.extension.annotations.SslCert
 * @since 1.0
 */
public interface SelfSignedCertificate {

    /**
     * The password used for all generated keystore and truststore files.
     * <p>
     * A random value is generated. This constant is provided for convenience when using APIs that require the
     * password directly, such as
     * {@link jakarta.ws.rs.client.ClientBuilder#keyStore(KeyStore, char[]) ClientBuilder.keyStore()}.
     * </p>
     */
    String KEYSTORE_PASSWORD = UUID.randomUUID().toString();

    /**
     * Returns the {@link SSLContext} configured for the client.
     * <p>
     * This context is automatically applied to injected {@link jakarta.ws.rs.client.Client Client} instances when
     * {@link dev.resteasy.junit.extension.annotations.SelfSignedCert @SelfSignedCert} is present on the test class
     * alongside {@link dev.resteasy.junit.extension.annotations.RestBootstrap @RestBootstrap} and no custom
     * {@link RestClientBuilderProvider} is specified via
     * {@link dev.resteasy.junit.extension.annotations.RestClientConfig @RestClientConfig}. When a custom provider is
     * used, it is responsible for configuring the client SSL context itself, typically via
     * {@link dev.resteasy.junit.extension.annotations.SslCert @SslCert} field injection.
     * </p>
     *
     * @return the client SSL context
     */
    SSLContext clientSslContext();

    /**
     * Returns the client {@link KeyStore} containing the client's private key and certificate.
     * <p>
     * This can be used directly with APIs that accept a {@link KeyStore}, such as
     * {@link jakarta.ws.rs.client.ClientBuilder#keyStore(KeyStore, char[]) ClientBuilder.keyStore()}.
     * </p>
     *
     * @return the client keystore
     */
    KeyStore clientKeyStore();

    /**
     * Returns the path to the client keystore file containing the client's private key and certificate.
     *
     * @return the path to the client keystore file
     */
    Path clientKeyStorePath();

    /**
     * Returns the client {@link KeyStore truststore} containing the server's certificate.
     * <p>
     * This can be used directly with APIs that accept a {@link KeyStore}, such as
     * {@link jakarta.ws.rs.client.ClientBuilder#trustStore(KeyStore) ClientBuilder.trustStore()}.
     * </p>
     *
     * @return the client truststore
     */
    KeyStore clientTrustStore();

    /**
     * Returns the path to the client truststore file containing the server's certificate.
     *
     * @return the path to the client truststore file
     */
    Path clientTrustStorePath();

    /**
     * Returns the {@link SSLContext} configured for the server.
     * <p>
     * This context is automatically applied to the {@link jakarta.ws.rs.SeBootstrap.Configuration SeBootstrap
     * Configuration} when {@link dev.resteasy.junit.extension.annotations.SelfSignedCert @SelfSignedCert} is present
     * on the test class alongside {@link dev.resteasy.junit.extension.annotations.RestBootstrap @RestBootstrap} and no
     * custom {@link ConfigurationProvider} is specified via
     * {@link dev.resteasy.junit.extension.annotations.RestBootstrap#configFactory() configFactory}. When a custom
     * provider is used, it is responsible for configuring the server SSL context itself, typically via
     * {@link dev.resteasy.junit.extension.annotations.SslCert @SslCert} field injection.
     * </p>
     *
     * @return the server SSL context
     */
    SSLContext serverSslContext();

    /**
     * Returns the server {@link KeyStore} containing the server's private key and certificate.
     * <p>
     * This can be used directly with APIs that accept a {@link KeyStore}, such as
     * {@link jakarta.ws.rs.client.ClientBuilder#keyStore(KeyStore, char[]) ClientBuilder.keyStore()}.
     * </p>
     *
     * @return the server keystore
     */
    KeyStore serverKeyStore();

    /**
     * Returns the path to the server keystore file containing the server's private key and certificate.
     *
     * @return the path to the server keystore file
     */
    Path serverKeyStorePath();

    /**
     * Returns the server {@link KeyStore truststore} containing the client's certificate.
     * <p>
     * This can be used directly with APIs that accept a {@link KeyStore}, such as
     * {@link jakarta.ws.rs.client.ClientBuilder#trustStore(KeyStore) ClientBuilder.trustStore()}.
     * </p>
     *
     * @return the server truststore
     */
    KeyStore serverTrustStore();

    /**
     * Returns the path to the server truststore file containing the client's certificate.
     *
     * @return the path to the server truststore file
     */
    Path serverTrustStorePath();

    /**
     * Returns the password used for all keystore and truststore files.
     *
     * @return the keystore password
     */
    String keyStorePassword();

    /**
     * Returns the keystore type used for all generated keystore and truststore files.
     *
     * @return the keystore type (e.g., {@code "PKCS12"})
     */
    String keyStoreType();
}
