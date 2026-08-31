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

import dev.resteasy.junit.extension.api.RestClientBuilderProvider;

/**
 * Allows an injected client to be built based on the configuration provider.
 * <p>
 * The default provider attempts to use a {@link java.util.ServiceLoader} to lookup the first provider found. If
 * found that provider will be used. This can be useful when you want to use the same provider across all tests
 * without having to define the type on each annotation.
 * </p>
 * <p>
 * In addition to, or instead of, a provider, individual Jakarta REST providers (filters, interceptors, message body
 * readers/writers, etc.) may be registered directly on the client via {@link #providers()}.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @since 1.0
 */
@Inherited
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface RestClientConfig {

    /**
     * The type of the configuration provider.
     *
     * @return the configuration provider
     */
    Class<? extends RestClientBuilderProvider> value() default RestClientBuilderProvider.class;

    /**
     * Additional Jakarta REST providers to register on the injected client, such as
     * {@linkplain jakarta.ws.rs.container.ContainerRequestFilter filters}, interceptors or
     * {@linkplain jakarta.ws.rs.ext.MessageBodyReader message body readers/writers}.
     * <p>
     * Each type is registered via {@link jakarta.ws.rs.client.ClientBuilder#register(Class)}, so the Jakarta REST
     * implementation is responsible for instantiating it. As a result each provider <strong>must have a public,
     * no-argument constructor</strong>. A provider that requires constructor arguments or additional setup should be
     * registered through a {@link #value() RestClientBuilderProvider} instead.
     * </p>
     * <p>
     * These providers are registered <em>after</em> the {@link #value()} provider has configured the
     * {@link jakarta.ws.rs.client.ClientBuilder}, allowing a shared {@code RestClientBuilderProvider} to be combined
     * with one or more providers specified at an individual injection point.
     * </p>
     *
     * @return the provider classes to register on the client
     */
    Class<?>[] providers() default {};
}
