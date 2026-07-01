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
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.core.Application;

import org.junit.jupiter.api.extension.ExtendWith;

import dev.resteasy.junit.extension.api.ConfigurationProvider;
import dev.resteasy.junit.extension.extensions.RestResourceProducerExtension;
import dev.resteasy.junit.extension.extensions.SeBootstrapExtension;
import dev.resteasy.junit.extension.extensions.UriBuilderParameterResolver;

/**
 * An annotation which starts a {@link jakarta.ws.rs.SeBootstrap.Instance} for unit testing.
 * <p>
 * There are two ways to specify what to bootstrap:
 * </p>
 * <ol>
 * <li><strong>Application class</strong> - Specify a custom {@link Application} class via {@link #application()}:
 *
 * <pre>
 * &#64;RestBootstrap(application = MyApplication.class)
 * public class MyTest {
 *     // Tests run with MyApplication
 * }
 * </pre>
 *
 * </li>
 * <li><strong>Resource classes</strong> - For simple cases, specify Jakarta REST resource classes via {@link #value()}:
 *
 * <pre>
 * &#64;RestBootstrap({ UserResource.class, OrderResource.class })
 * public class MyTest {
 *     // Tests run with a synthetic Application containing these resources
 * }
 * </pre>
 *
 * </li>
 * </ol>
 * <p>
 * Exactly one of {@link #application()} or {@link #value()} must be specified. Specifying both or neither will result
 * in an {@link org.junit.jupiter.api.extension.ExtensionConfigurationException}.
 * </p>
 * <p>
 * The default provider attempts to use a {@link java.util.ServiceLoader} to lookup the first provider found. If
 * found that provider will be used. This can be useful when when you want to use the same provider across all tests
 * without having to define the type on each annotation.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith({
        SeBootstrapExtension.class,
        RestResourceProducerExtension.class,
        UriBuilderParameterResolver.class,
})
public @interface RestBootstrap {

    /**
     * Jakarta REST resource classes to bootstrap for testing.
     * <p>
     * This provides a simplified alternative to {@link #application()} for common cases where you only need to specify
     * resource classes without custom {@link Application} configuration. When {@code value()} is specified,
     * the extension creates a synthetic {@link Application} that returns these classes from
     * {@link Application#getClasses()}.
     * </p>
     * <p>
     * This is mutually exclusive with {@link #application()}. Exactly one must be specified.
     * </p>
     *
     * <h3>Example</h3>
     *
     * <pre>
     * &#64;RestBootstrap({ UserResource.class, OrderResource.class })
     * public class SimpleTest {
     *     &#64;RestResource
     *     private Client client;
     *
     *     &#64;Test
     *     public void testUser() {
     *         // Test UserResource
     *     }
     * }
     * </pre>
     *
     * <p>
     * <strong>When to use {@code value()} vs {@code application()}:</strong>
     * </p>
     * <ul>
     * <li>Use {@code value()} for simple tests that only need to specify resource classes</li>
     * <li>Use {@code application()} when you need:
     * <ul>
     * <li>Custom {@link jakarta.ws.rs.ApplicationPath} configuration</li>
     * <li>Custom {@link Application} properties</li>
     * <li>Programmatic resource filtering or dynamic configuration</li>
     * </ul>
     * </li>
     * </ul>
     *
     * @return an array of Jakarta REST resource classes, or empty if using {@link #application()} instead
     */
    Class<?>[] value() default {};

    /**
     * The {@linkplain Application application} to use for
     * {@linkplain SeBootstrap#start(Class, SeBootstrap.Configuration) starting} a
     * {@link SeBootstrap.Instance}.
     * <p>
     * This is mutually exclusive with {@link #value()}. Exactly one must be specified.
     * </p>
     * <p>
     * The default value of {@link Application}.class serves as a marker indicating no application was specified.
     * In this case, {@link #value()} must be non-empty.
     * </p>
     *
     * @return the application class, or {@link Application}.class if using {@link #value()} instead
     */
    Class<? extends Application> application() default Application.class;

    /**
     * A factory used to be build the {@linkplain SeBootstrap.Configuration configuration} for starting the
     * {@link SeBootstrap}.
     *
     * @return the configuration factory to use
     */
    Class<? extends ConfigurationProvider> configFactory() default ConfigurationProvider.class;

    /**
     * The timeout used to wait for the {@link SeBootstrap} to start.
     *
     * @return the timeout value
     */
    long timeout() default 60L;

    /**
     * The time unit to use for the timeout.
     *
     * @return the timeout unit
     */
    TimeUnit timeoutUnit() default TimeUnit.SECONDS;
}
