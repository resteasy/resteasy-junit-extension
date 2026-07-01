/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.api;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Enables injection of custom types into test classes.
 * <p>
 * Implementations of this interface can provide custom objects for injection into test fields (static or instance) and
 * method parameters (constructor, lifecycle methods, or test methods). All injectable fields and parameters must be
 * annotated with {@link dev.resteasy.junit.extension.annotations.RestResource @RestResource}.
 * </p>
 *
 * <h2>Built-in Producers</h2>
 * <p>
 * The extension provides built-in producers for:
 * </p>
 * <ul>
 * <li>{@link jakarta.ws.rs.SeBootstrap.Configuration} - The SeBootstrap configuration</li>
 * <li>{@link jakarta.ws.rs.client.Client} - REST client instance</li>
 * <li>{@link jakarta.ws.rs.client.WebTarget} - WebTarget (optionally qualified with {@code @RequestPath})</li>
 * <li>{@link jakarta.ws.rs.core.UriBuilder} - URI builder</li>
 * <li>{@link java.net.URI} - Base URI (optionally qualified with {@code @RequestPath})</li>
 * </ul>
 *
 * <h2>Custom Producer Example</h2>
 * <p>
 * To inject custom types, implement this interface and register via {@link java.util.ServiceLoader}:
 * </p>
 *
 * <pre>
 * public class DataSourceProducer implements RestResourceProducer {
 *     &#64;Override
 *     public boolean canInject(ExtensionContext context, Class&lt;?&gt; clazz, Annotation... qualifiers) {
 *         return DataSource.class.isAssignableFrom(clazz);
 *     }
 *
 *     &#64;Override
 *     public Object produce(ExtensionContext context, Class&lt;?&gt; clazz, Annotation... qualifiers) {
 *         if (!canInject(context, clazz, qualifiers)) {
 *             throw new ParameterResolutionException("Cannot produce type: " + clazz);
 *         }
 *         // Create and return your DataSource instance
 *         return createDataSource();
 *     }
 *
 *     private DataSource createDataSource() {
 *         // Implementation details...
 *     }
 * }
 *
 * // Now you can inject DataSource in tests:
 * &#64;RestBootstrap(MyApp.class)
 * public class MyTest {
 *     &#64;RestResource
 *     private DataSource dataSource;
 *
 *     &#64;Test
 *     public void testDatabase() {
 *         // Use dataSource...
 *     }
 * }
 * </pre>
 *
 * <h2>Registration</h2>
 * <p>
 * Custom producers must be registered via {@link java.util.ServiceLoader} by creating a file
 * {@code META-INF/services/dev.resteasy.junit.extension.api.RestResourceProducer} containing the fully-qualified class
 * name of your implementation.
 * </p>
 *
 * <h2>Lifecycle</h2>
 * <p>
 * If the produced object implements {@link AutoCloseable}, it will be automatically closed when the test context ends.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @see dev.resteasy.junit.extension.annotations.RestResource
 * @since 1.0.0
 */
public interface RestResourceProducer {

    /**
     * Defines the lifecycle scope for resources produced by a {@link RestResourceProducer}.
     * <p>
     * The scope determines when {@link AutoCloseable} resources are automatically closed after injection.
     * This allows producers to balance performance (class-scoped reuse) with isolation (method-scoped cleanup).
     * </p>
     *
     * @since 1.0.0
     */
    enum Scope {
        /**
         * Use the natural scope of the injection point.
         * <ul>
         * <li><b>Static fields:</b> Cleaned up when the test class completes</li>
         * <li><b>Instance fields:</b> Cleaned up when the test class completes</li>
         * <li><b>Method parameters:</b> Cleaned up when the test method completes</li>
         * </ul>
         * <p>
         * This is the default scope and is appropriate for most custom producers where method-level isolation
         * is desired for parameters (e.g., temporary files, test-specific resources).
         * </p>
         *
         * @see #CLASS
         */
        DEFAULT,

        /**
         * Always use class scope for all injection points.
         * <p>
         * Resources are cleaned up when the test class completes, regardless of whether injected into a field
         * or method parameter. This scope is appropriate for expensive-to-create resources that are safe to
         * share across test methods.
         * </p>
         * <p>
         * <b>Example use cases:</b>
         * </p>
         * <ul>
         * <li>REST clients (thread-safe, expensive to create)</li>
         * <li>Database connection pools (designed for reuse)</li>
         * <li>Immutable configuration objects</li>
         * </ul>
         * <p>
         * Built-in producers ({@code Client}, {@code URI}, {@code Configuration}) use {@code CLASS} scope for
         * performance.
         * </p>
         *
         * @see #DEFAULT
         * @see #NEW
         */
        CLASS,

        /**
         * Create a new instance for every injection point.
         * <p>
         * The producer's {@link #produce(ExtensionContext, Class, Annotation...)} method is invoked every time
         * the resource is injected, completely bypassing any caching. This means:
         * </p>
         * <ul>
         * <li>Multiple field injections create separate instances</li>
         * <li>Each method parameter injection creates a fresh instance</li>
         * <li>No instance is ever shared or reused</li>
         * </ul>
         * <p>
         * This scope is appropriate for mutable or stateful resources that must be isolated between injection
         * points to prevent cross-contamination or unexpected behavior.
         * </p>
         * <p>
         * <b>Example use cases:</b>
         * </p>
         * <ul>
         * <li>Mutable API clients that maintain request state (e.g., {@code WebTarget})</li>
         * <li>Builder instances that accumulate state across method calls</li>
         * <li>Resources with per-injection configuration requirements</li>
         * </ul>
         * <p>
         * <b>Performance consideration:</b> Since {@code produce()} is called for every injection, this scope
         * should only be used when instance isolation is required. Use {@link #CLASS} for expensive,
         * thread-safe resources.
         * </p>
         * <p>
         * Built-in producers that use {@code NEW} scope: {@code WebTarget}
         * </p>
         *
         * @see #DEFAULT
         * @see #CLASS
         * @since 1.0.0
         */
        NEW
    }

    /**
     * Determines whether this producer can inject the specified type.
     * <p>
     * This method is called by the extension to find the appropriate producer for each injection point. It should
     * return {@code true} only if this producer can create instances of the given type, optionally considering any
     * qualifiers present.
     * </p>
     *
     * @param context    the current JUnit extension context, provides access to the test class and instance
     * @param clazz      the type being requested for injection (field type or parameter type)
     * @param qualifiers annotation qualifiers present on the injection point (e.g., {@code @RequestPath},
     *                   {@code @RestClientConfig})
     *
     * @return {@code true} if this producer can create instances of the given type, {@code false} otherwise
     */
    boolean canInject(ExtensionContext context, Class<?> clazz, Annotation... qualifiers);

    /**
     * Produces an instance of the requested type for injection.
     * <p>
     * This method is called when {@link #canInject(ExtensionContext, Class, Annotation...)} returns {@code true} for
     * the given type. It should create and return an appropriate instance. If the returned object implements
     * {@link AutoCloseable}, it will be automatically closed when the appropriate test context ends.
     * </p>
     * <p>
     * Implementations should validate that they can produce the requested type and throw
     * {@link IllegalArgumentException} if called with an unsupported type.
     * </p>
     *
     * @param context    the current JUnit extension context, provides access to the test class and instance
     * @param clazz      the type of the field or parameter to inject
     * @param qualifiers annotation qualifiers present on the injection point (e.g., {@code @RequestPath},
     *                   {@code @RestClientConfig})
     *
     * @return the newly created object to be injected, or {@code null} if it could not be produced
     *
     * @throws IllegalArgumentException if the type cannot be produced by this producer (implementations should validate
     *                                  via {@link #canInject} first)
     */
    Object produce(ExtensionContext context, Class<?> clazz, Annotation... qualifiers) throws IllegalArgumentException;

    /**
     * Returns the lifecycle scope for resources produced by this producer.
     * <p>
     * The default implementation returns {@link Scope#DEFAULT}, which uses the natural scope of the injection point:
     * method parameters are cleaned up after each test method, while fields are cleaned up when the test class completes.
     * </p>
     * <p>
     * Producers can override this method to return {@link Scope#CLASS} for expensive-to-create resources that should
     * be shared across all test methods in a class.
     * </p>
     *
     * @return the scope for resources produced by this producer
     */
    default Scope scope() {
        return Scope.DEFAULT;
    }
}
