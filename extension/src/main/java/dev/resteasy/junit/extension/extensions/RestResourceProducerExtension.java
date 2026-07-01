/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;

import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.api.RestResourceProducer;

/**
 * An extension for using {@linkplain RestResourceProducer producers} to inject fields annotated with
 * {@link RestResource} and method or constructor parameters.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class RestResourceProducerExtension
        implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private static final Logger LOGGER = Logger.getLogger(RestResourceProducerExtension.class);

    private final ServiceLoader<RestResourceProducer> producers;
    private final List<AutoCloseable> newResources;
    private final Lock lock = new ReentrantLock();

    public RestResourceProducerExtension() {
        producers = ServiceLoader.load(RestResourceProducer.class);
        newResources = new CopyOnWriteArrayList<>();
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
    public void afterEach(final ExtensionContext context) {
        final List<AutoCloseable> resources;
        lock.lock();
        try {
            resources = List.copyOf(newResources);
            newResources.clear();
        } finally {
            lock.unlock();
        }
        resources.forEach(closeable -> {
            try {
                closeable.close();
            } catch (final Exception e) {
                LOGGER.debugf(e, "Exception while closing %s", closeable);
            }
        });
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

        for (RestResourceProducer producer : producers) {
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

        RestResourceProducer producer = getProducer(lexicalContext, parameterContext.getParameter().getType(),
                parameterContext.getParameter().getAnnotations());

        if (producer == null) {
            return null;
        }

        try {
            // Pass the lexical context to the producer!
            return resolveValue(lexicalContext, producer, parameterContext.getParameter()
                    .getType(), parameterContext.getParameter().getAnnotations(), true);
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
            RestResourceProducer resourceProducer = null;
            for (RestResourceProducer producer : producers) {
                if (producer.canInject(context, field.getType(), field.getAnnotations())) {
                    resourceProducer = producer;
                    break;
                }
            }
            if (resourceProducer == null) {
                throw new ExtensionConfigurationException(
                        String.format("Could not find RestResourceProducer for field '%s' of type %s.", field, field.getType()
                                .getName()));
            }
            try {
                final Object value = resolveValue(context, resourceProducer, field.getType(), field.getAnnotations(), false,
                        Modifier.isStatic(field.getModifiers()));
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

    private RestResourceProducer getProducer(ExtensionContext context, Class<?> type,
            Annotation[] annotations) {
        for (RestResourceProducer producer : producers) {
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

    private Object resolveValue(final ExtensionContext context, final RestResourceProducer producer, final Class<?> clazz,
            final Annotation[] annotations, final boolean isParameter) {
        return resolveValue(context, producer, clazz, annotations, isParameter, false);
    }

    private Object resolveValue(final ExtensionContext context, final RestResourceProducer producer, final Class<?> clazz,
            final Annotation[] annotations, final boolean isParameter, final boolean isStatic) {
        final RestResourceProducer.Scope scope = producer.scope();
        if (scope == RestResourceProducer.Scope.NEW) {
            final Object value = producer.produce(context, clazz, annotations);
            if (value instanceof AutoCloseable) {
                if (isStatic) {
                    context.getStore(ExtensionContext.Namespace.create(RestResourceProducerExtension.class))
                            .put(UUID.randomUUID().toString(), value);
                } else {
                    lock.lock();
                    try {
                        newResources.add((AutoCloseable) value);
                    } finally {
                        lock.unlock();
                    }
                }
            }
            return value;
        }
        final ExtensionContext.Store store;
        if (scope == RestResourceProducer.Scope.DEFAULT && isParameter) {
            store = context
                    .getStore(ExtensionContext.Namespace.create(RestResourceProducerExtension.class));
        } else {
            store = ClassContext.getStore(context);
        }
        return store.getOrComputeIfAbsent(CacheKey.of(producer.getClass(), scope, annotations),
                (key) -> producer.produce(context, clazz, annotations));
    }

    private static class CacheKey {
        private final Class<?> type;
        private final RestResourceProducer.Scope scope;
        private final Annotation[] annotations;

        private CacheKey(final Class<?> type, final RestResourceProducer.Scope scope, final Annotation[] annotations) {
            this.type = type;
            this.scope = scope;
            this.annotations = annotations;
        }

        static CacheKey of(final Class<?> type, final RestResourceProducer.Scope scope, final Annotation[] annotations) {
            return new CacheKey(type, scope, annotations);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, scope, Arrays.hashCode(annotations));
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof CacheKey)) {
                return false;
            }
            final CacheKey other = (CacheKey) obj;
            return Objects.equals(type, other.type)
                    && Objects.equals(scope, other.scope)
                    && Arrays.equals(annotations, other.annotations);
        }

        @Override
        public String toString() {
            return "CacheKey[type=" + type + ", scope=" + scope + ", annotations=" + Arrays.toString(annotations) + "]";
        }
    }
}
