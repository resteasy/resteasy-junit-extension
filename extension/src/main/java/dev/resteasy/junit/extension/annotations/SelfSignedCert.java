/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import dev.resteasy.junit.extension.extensions.SelfSignedCertificateExtension;

/**
 * Enables generation of self-signed certificates for SSL/TLS testing.
 * <p>
 * When placed on a test class, this annotation triggers the generation of self-signed certificates for both
 * server and client. The generated {@link dev.resteasy.junit.extension.api.SelfSignedCertificate SelfSignedCertificate}
 * can be injected into test fields annotated with {@link SslCert @SslCert} or into test method parameters by type.
 * </p>
 *
 * <h2>Standalone Usage</h2>
 * <p>
 * This annotation can be used independently of {@link RestBootstrap @RestBootstrap} when only the certificate
 * artifacts are needed (e.g., for configuring a non-Jakarta REST client or an external server):
 * </p>
 *
 * <pre>
 * &#64;SelfSignedCert
 * public class ClientTest {
 *     &#64;SslCert
 *     private SelfSignedCertificate certificate;
 *
 *     &#64;Test
 *     public void testClient() {
 *         SSLContext clientCtx = certificate.clientSslContext();
 *         // configure a custom client with the SSL context
 *     }
 * }
 * </pre>
 *
 * <h2>Usage with {@code @RestBootstrap}</h2>
 * <p>
 * When combined with {@link RestBootstrap @RestBootstrap}, the server is automatically started with HTTPS using
 * the generated server SSL context, and injected {@link jakarta.ws.rs.client.Client Client} instances are
 * configured with the client SSL context:
 * </p>
 *
 * <pre>
 * &#64;SelfSignedCert
 * &#64;RestBootstrap(MyResource.class)
 * public class SslTest {
 *     &#64;RestResource
 *     private Client client; // auto-configured with client SSL context
 *
 *     &#64;Test
 *     public void testHttps(URI baseUri) {
 *         assertEquals("https", baseUri.getScheme());
 *     }
 * }
 * </pre>
 *
 * <p>
 * Tests annotated with this annotation are automatically tagged with {@code ssl-test}, allowing them to be
 * included or excluded using JUnit {@link org.junit.jupiter.api.Tag tag filtering}.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @see SslCert
 * @see dev.resteasy.junit.extension.api.SelfSignedCertificate
 * @since 1.0
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(SelfSignedCertificateExtension.class)
@Tag("ssl-test")
public @interface SelfSignedCert {
}
