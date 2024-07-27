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
import dev.resteasy.junit.extension.extensions.InjectionProducerExtension;
import dev.resteasy.junit.extension.extensions.SeBootstrapExtension;

/**
 * An annotation which starts a {@link jakarta.ws.rs.SeBootstrap.Instance} for unit testing.
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
        InjectionProducerExtension.class
})
public @interface RestBootstrap {

    /**
     * The {@linkplain Application application} to use for
     * {@linkplain SeBootstrap#start(Class, SeBootstrap.Configuration) starting} a
     * {@link SeBootstrap.Instance}.
     *
     * @return the application class
     */
    Class<? extends Application> value();

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
     * @return the timout value
     */
    long timeout() default 60L;

    /**
     * The time unit to use for the timeout.
     *
     * @return the timeout unit
     */
    TimeUnit timoutUnit() default TimeUnit.SECONDS;
}
