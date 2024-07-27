/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.api;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * An implementation of this interface is used to inject static fields, instance fields and parameters. For static
 * fields and instance fields, the field must be annotated with {@link jakarta.inject.Inject}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface InjectionProducer {

    /**
     * Indicates the type can be produced from this producer.
     *
     * @param clazz the type to check
     *
     * @return {@code true} if this producer can create this type, otherwise {@code false}
     */
    boolean canInject(Class<?> clazz);

    /**
     * Creates the object which can be injected into a static field, an instance field or a parameter.
     *
     * @param context    the current extension context
     * @param clazz      the type of the field or parameter
     * @param qualifiers the qualifiers, if any, for the field or parameter
     *
     * @return the newly constructed object or {@code null} if it could not be produced
     *
     * @throws IllegalArgumentException if the type cannot be assigned to the type this producer can produce
     */
    Object produce(ExtensionContext context, Class<?> clazz, Annotation... qualifiers) throws IllegalArgumentException;
}
