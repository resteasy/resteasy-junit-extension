/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field for injection of a {@link dev.resteasy.junit.extension.api.SelfSignedCertificate SelfSignedCertificate}.
 * <p>
 * The test class must also be annotated with {@link SelfSignedCert @SelfSignedCert} to trigger certificate generation.
 * </p>
 *
 * <pre>
 * &#64;SelfSignedCert
 * public class SslTest {
 *     &#64;SslCert
 *     private SelfSignedCertificate certificate;
 *
 *     &#64;Test
 *     public void testSsl() {
 *         SSLContext ctx = certificate.clientSslContext();
 *         // ...
 *     }
 * }
 * </pre>
 *
 * <p>
 * This annotation can also be used on fields in {@link dev.resteasy.junit.extension.api.ConfigurationProvider
 * ConfigurationProvider} and {@link dev.resteasy.junit.extension.api.RestClientBuilderProvider
 * RestClientBuilderProvider} implementations to receive the generated certificate for custom SSL configuration:
 * </p>
 *
 * <pre>
 * public class CustomSslConfigProvider implements ConfigurationProvider {
 *     &#64;SslCert
 *     private SelfSignedCertificate certificate;
 *
 *     &#64;Override
 *     public Configuration getConfiguration(ExtensionContext context) {
 *         return Configuration.builder()
 *                 .protocol("HTTPS")
 *                 .sslContext(certificate.serverSslContext())
 *                 .build();
 *     }
 * }
 * </pre>
 *
 * <p>
 * For test method parameters, injection is resolved by type without requiring this annotation.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @see SelfSignedCert
 * @see dev.resteasy.junit.extension.api.SelfSignedCertificate
 * @since 1.0
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SslCert {
}
