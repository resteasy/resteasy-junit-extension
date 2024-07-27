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
 * Allows an injected client to be build based on the configuration provider.
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
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RestClientConfig {

    /**
     * The type of the configuration provider.
     *
     * @return the configuration provider
     */
    Class<? extends RestClientBuilderProvider> value() default RestClientBuilderProvider.class;
}
