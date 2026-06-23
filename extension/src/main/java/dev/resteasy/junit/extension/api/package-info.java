/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Extension API and SPI for customizing the RESTEasy JUnit extension.
 * <p>
 * This package contains interfaces that allow users to extend and customize the behavior of the RESTEasy JUnit
 * extension through Service Provider Interfaces (SPI).
 * </p>
 *
 * <h2>SPI Interfaces</h2>
 * <ul>
 * <li>{@link dev.resteasy.junit.extension.api.ConfigurationProvider ConfigurationProvider} - Provides custom
 * {@link jakarta.ws.rs.SeBootstrap.Configuration} for test instances</li>
 * <li>{@link dev.resteasy.junit.extension.api.RestClientBuilderProvider RestClientBuilderProvider} - Provides custom
 * {@link jakarta.ws.rs.client.ClientBuilder} configuration</li>
 * <li>{@link dev.resteasy.junit.extension.api.InjectionProducer InjectionProducer} - Enables injection of custom types
 * into test classes</li>
 * </ul>
 *
 * <h2>Service Registration</h2>
 * <p>
 * All SPI implementations must be registered via Java's {@link java.util.ServiceLoader} mechanism by creating a file in
 * {@code META-INF/services/} with the fully-qualified interface name.
 * </p>
 *
 * <h2>Example: Custom Configuration Provider</h2>
 *
 * <pre>
 * &#64;MetaInfServices
 * public class CustomPortProvider implements ConfigurationProvider {
 *     &#64;Override
 *     public SeBootstrap.Configuration getConfiguration() {
 *         return SeBootstrap.Configuration.builder()
 *                 .port(9090)
 *                 .build();
 *     }
 * }
 *
 * &#64;RestBootstrap(value = MyApp.class, configFactory = CustomPortProvider.class)
 * public class MyTest {
 *     // Test will run on port 9090
 * }
 * </pre>
 *
 * <h2>Example: Custom Injection Producer</h2>
 *
 * <pre>
 * &#64;MetaInfServices
 * public class DatabaseProducer implements InjectionProducer {
 *     &#64;Override
 *     public boolean canInject(ExtensionContext context, Class&lt;?&gt; clazz, Annotation... qualifiers) {
 *         return DataSource.class.isAssignableFrom(clazz);
 *     }
 *
 *     &#64;Override
 *     public Object produce(ExtensionContext context, Class&lt;?&gt; clazz, Annotation... qualifiers) {
 *         // Create and return DataSource
 *     }
 * }
 *
 * // Now you can inject DataSource in tests:
 * &#64;RestBootstrap(MyApp.class)
 * public class MyTest {
 *     &#64;RestResource
 *     private DataSource dataSource;
 * }
 * </pre>
 *
 * @see dev.resteasy.junit.extension.annotations
 * @since 1.0.0
 */
package dev.resteasy.junit.extension.api;
