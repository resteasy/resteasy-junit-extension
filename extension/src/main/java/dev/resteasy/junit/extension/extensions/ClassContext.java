/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContextException;

import dev.resteasy.junit.extension.annotations.RestBootstrap;

/**
 * A utility which creates a store at lives the lifecycle of a class.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class ClassContext {

    /**
     * Gets the store for the current test class.
     *
     * @param context the context used to determine the class context
     *
     * @return the context store
     */
    static ExtensionContext.Store getStore(final ExtensionContext context) {
        final ExtensionContext usingContext = resolveContext(context);
        return usingContext
                .getStore(ExtensionContext.Namespace.create(usingContext.getRequiredTestClass(), usingContext.getUniqueId()));
    }

    private static ExtensionContext resolveContext(final ExtensionContext context) {
        // Walk up the parent chain to find a class with @RestBootstrap
        ExtensionContext current = context.getTestMethod().isPresent()
                ? context.getParent().orElse(context)
                : context;
        while (current != null) {
            // To be a class level context we must have a TestClass, but no method.
            final boolean isClassContext = current.getTestClass().isPresent() && current.getTestMethod().isEmpty();

            if (isClassContext) {
                final Class<?> clazz = current.getRequiredTestClass();
                // Strictly check this specific class, preventing JUnit from reading the enclosing outer class
                final boolean hasAnnotation = clazz.isAnnotationPresent(RestBootstrap.class) ||
                        Stream.of(clazz.getAnnotations())
                                .anyMatch(a -> a.annotationType().isAnnotationPresent(RestBootstrap.class));
                if (hasAnnotation) {
                    // Found it, use this context's store
                    return current;
                }
            }
            // Keep walking up
            current = current.getParent().orElse(null);
        }
        throw new ExtensionContextException(String.format("Could not find context for test class %s",
                context.getTestClass().map(Class::getName).orElse("<unknown>")));
    }
}
