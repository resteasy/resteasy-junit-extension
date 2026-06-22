/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ServiceLoader;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Predicate;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;

import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.api.InjectionProducer;

/**
 * An extension for using {@linkplain InjectionProducer producers} to inject fields annotated with
 * {@link RestResource} and method or constructor parameters.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class InjectionProducerExtension
        implements BeforeAllCallback, BeforeEachCallback, ParameterResolver {

    private final ServiceLoader<InjectionProducer> producers;

    public InjectionProducerExtension() {
        producers = ServiceLoader.load(InjectionProducer.class);
    }

    @Override
    public void beforeAll(final ExtensionContext context) {
        injectStaticFields(context, context.getRequiredTestClass());
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        context.getRequiredTestInstances().getAllInstances()
                .forEach(instance -> injectInstanceFields(context, instance));
    }

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (!parameterContext.isAnnotated(RestResource.class)) {
            return false;
        }
        // Resolve lexical context for the parameter
        final Class<?> declaringClass = parameterContext.getDeclaringExecutable().getDeclaringClass();
        final ExtensionContext lexicalContext = resolveLexicalContext(extensionContext, declaringClass);

        for (InjectionProducer producer : producers) {
            if (producer.canInject(lexicalContext, parameterContext.getParameter().getType(),
                    parameterContext.getParameter().getAnnotations())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (!parameterContext.isAnnotated(RestResource.class)) {
            return null;
        }
        // Resolve lexical context for the parameter
        final Class<?> declaringClass = parameterContext.getDeclaringExecutable().getDeclaringClass();
        final ExtensionContext lexicalContext = resolveLexicalContext(extensionContext, declaringClass);

        InjectionProducer injectionProducer = getProducer(lexicalContext, parameterContext.getParameter().getType(),
                parameterContext.getParameter().getAnnotations());

        if (injectionProducer == null) {
            return null;
        }

        try {
            // Pass the lexical context to the producer!
            final Object value = injectionProducer.produce(lexicalContext, parameterContext.getParameter().getType(),
                    parameterContext.getParameter().getAnnotations());

            trackResource(lexicalContext, value);
            return value;
        } catch (Throwable e) {
            throw new ParameterResolutionException(
                    String.format("Failed to resolve parameter '%s'.", parameterContext.getParameter()), e);
        }
    }

    private void injectStaticFields(final ExtensionContext context, final Class<?> testClass) {
        injectFields(context, null, testClass, (f) -> Modifier.isStatic(f.getModifiers()));
    }

    private void injectInstanceFields(final ExtensionContext context, final Object instance) {
        injectFields(context, instance, instance.getClass(), (f) -> !Modifier.isStatic(f.getModifiers()));
    }

    private void injectFields(final ExtensionContext context, final Object testInstance, final Class<?> testClass,
            final Predicate<Field> predicate) {

        AnnotationSupport.findAnnotatedFields(testClass, RestResource.class, predicate).forEach(field -> {
            if (Modifier.isFinal(field.getModifiers())) {
                throw new ExtensionConfigurationException(
                        String.format("Field '%s' cannot be final for injecting a REST resource.", field));
            }
            // Find the producer which can provide this parameter
            InjectionProducer injectionProducer = null;
            for (InjectionProducer producer : producers) {
                if (producer.canInject(context, field.getType(), field.getAnnotations())) {
                    injectionProducer = producer;
                    break;
                }
            }
            if (injectionProducer == null) {
                throw new ExtensionConfigurationException(
                        String.format("Could not find InjectionProducer for field '%s' of type %s.", field, field.getType()
                                .getName()));
            }
            try {
                final Object value = injectionProducer.produce(context, field.getType(), field.getAnnotations());
                trackResource(context, value);
                if (field.trySetAccessible()) {
                    field.set(testInstance, value);
                } else {
                    throw new ParameterResolutionException(
                            String.format("Could not make field %s accessible for injection.", field));
                }
            } catch (Throwable e) {
                if (e instanceof ParameterResolutionException) {
                    throw (ParameterResolutionException) e;
                }
                throw new ParameterResolutionException(
                        String.format("Could not make field %s accessible for injection.", field), e);
            }
        });
    }

    private InjectionProducer getProducer(ExtensionContext context, Class<?> type,
            java.lang.annotation.Annotation[] annotations) {
        for (InjectionProducer producer : producers) {
            if (producer.canInject(context, type, annotations)) {
                return producer;
            }
        }
        return null;
    }

    /**
     * Walks up the context tree to find the context that strictly matches the class
     * where the parameter or field was written in the source code.
     */
    private ExtensionContext resolveLexicalContext(final ExtensionContext runtimeContext, final Class<?> declaringClass) {
        ExtensionContext current = runtimeContext;
        while (current != null) {
            if (current.getTestClass().isPresent() && current.getTestClass().get().equals(declaringClass)) {
                return current;
            }
            current = current.getParent().orElse(null);
        }
        // Fallback to runtime context if somehow not found in the hierarchy
        return runtimeContext;
    }

    /**
     * Tracks resources in the specific context's Store so they are closed exactly when THAT context ends.
     */
    private void trackResource(final ExtensionContext context, final Object value) {
        if (value instanceof AutoCloseable) {
            ResourceTracker tracker = ClassContext.getStore(context).getOrComputeIfAbsent(ResourceTracker.class,
                    k -> new ResourceTracker(), ResourceTracker.class);
            tracker.resources.add((AutoCloseable) value);
        }
    }

    // Completely replaces the AfterAllCallback, guaranteeing thread-safe, context-isolated teardowns
    private static class ResourceTracker implements ExtensionContext.Store.CloseableResource, AutoCloseable {
        private final BlockingDeque<AutoCloseable> resources = new LinkedBlockingDeque<>();

        @Override
        public void close() {
            AutoCloseable closeable;
            while ((closeable = resources.pollFirst()) != null) {
                try {
                    closeable.close();
                } catch (Throwable t) {
                    // Ignored during teardown
                }
            }
        }
    }
}
