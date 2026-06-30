/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Provides a JUnit extension for testing Jakarta REST applications using {@code SeBootstrap}.
 * <p>
 * This module enables seamless integration testing of Jakarta REST endpoints by managing the
 * {@link jakarta.ws.rs.SeBootstrap SeBootstrap} lifecycle and providing dependency injection
 * for test resources.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *     <li>Automatic {@code SeBootstrap} instance management for test classes</li>
 *     <li>Injection of REST clients, URIs, and other test resources</li>
 *     <li>Support for custom server configuration via properties or providers</li>
 *     <li>Extensible via {@link java.util.ServiceLoader ServiceLoader} SPI</li>
 *     <li>Implementation-agnostic - works with any Jakarta REST 3.1+ implementation</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <!-- @formatter:off -->
 * {@snippet :
 * @RestBootstrap(MyApplication.class)
 * public class HelloResourceTest {
 *
 *     @RestResource
 *     private Client client;
 *
 *     @RestResource
 *     private URI baseUri;
 *
 *     @Test
 *     public void testHello() {
 *         Response response = client.target(baseUri)
 *             .path("/hello")
 *             .request()
 *             .get();
 *
 *         assertEquals(200, response.getStatus());
 *     }
 * }
 * }
 * <!-- @formatter:on -->
 *
 * <h2>Exported Packages</h2>
 * <dl>
 *     <dt>{@link dev.resteasy.junit.extension.annotations}</dt>
 *     <dd>Primary annotations for marking test classes and resources
 *         ({@link dev.resteasy.junit.extension.annotations.RestBootstrap @RestBootstrap},
 *         {@link dev.resteasy.junit.extension.annotations.RestResource @RestResource},
 *         {@link dev.resteasy.junit.extension.annotations.RequestPath @RequestPath})</dd>
 *
 *     <dt>{@link dev.resteasy.junit.extension.api}</dt>
 *     <dd>Extension SPI for custom configuration providers and resource producers</dd>
 * </dl>
 *
 * <h2>Service Provider Interfaces</h2>
 * <p>
 * This module uses the following services via {@link java.util.ServiceLoader ServiceLoader}:
 * </p>
 * <ul>
 *     <li>{@link dev.resteasy.junit.extension.api.ConfigurationProvider} - Custom SeBootstrap configuration</li>
 *     <li>{@link dev.resteasy.junit.extension.api.RestResourceProducer} - Custom injectable resource types</li>
 *     <li>{@link dev.resteasy.junit.extension.api.RestClientBuilderProvider} - Custom REST client configuration</li>
 * </ul>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @since 1.0.0
 * @see dev.resteasy.junit.extension.annotations.RestBootstrap
 * @see dev.resteasy.junit.extension.annotations.RestResource
 */
module dev.resteasy.junit.extension {

    requires transitive jakarta.ws.rs;
    requires transitive org.junit.jupiter.api;
    requires org.jboss.logging;

    exports dev.resteasy.junit.extension.annotations;
    exports dev.resteasy.junit.extension.api;

    uses dev.resteasy.junit.extension.api.ConfigurationProvider;
    uses dev.resteasy.junit.extension.api.RestResourceProducer;
    uses dev.resteasy.junit.extension.api.RestClientBuilderProvider;

    provides dev.resteasy.junit.extension.api.RestResourceProducer with
            dev.resteasy.junit.extension.extensions.ConfigurationProducer,
            dev.resteasy.junit.extension.extensions.RestClientProducer,
            dev.resteasy.junit.extension.extensions.UriProducer,
            dev.resteasy.junit.extension.extensions.WebTargetProducer;

    opens dev.resteasy.junit.extension.extensions to org.junit.platform.commons;
}
