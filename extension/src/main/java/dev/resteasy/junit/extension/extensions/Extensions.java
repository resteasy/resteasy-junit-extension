/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.ExtensionContextException;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class Extensions {

    @SuppressWarnings("unchecked")
    static <T> T createProvider(final Class<? extends T> factoryType, final Class<T> interfaceType,
            final Supplier<T> defaultProvider) {
        if (factoryType == interfaceType) {
            return loadProvider(interfaceType, defaultProvider);
        }

        if (factoryType.isInterface()) {
            final Class<?>[] interfaces = { factoryType };
            return (T) Proxy.newProxyInstance(factoryType.getClassLoader(), interfaces,
                    (proxy, method, args) -> MethodHandles
                            .privateLookupIn(factoryType, MethodHandles.lookup())
                            .in(factoryType)
                            .unreflectSpecial(method, factoryType)
                            .bindTo(proxy)
                            .invokeWithArguments(args));
        } else {
            try {
                final Constructor<? extends T> constructor = factoryType.getConstructor();
                return constructor.newInstance();
            } catch (InvocationTargetException | InstantiationException e) {
                throw new ExtensionContextException(String.format("Failed to create provider %s", factoryType.getName()), e);
            } catch (NoSuchMethodException e) {
                throw new ExtensionContextException(
                        String.format("Failed to find no-arg constructor for type %s", factoryType.getName()), e);
            } catch (IllegalAccessException e) {
                throw new ExtensionContextException(String.format("Constructor for %s is not public.", factoryType.getName()),
                        e);
            }
        }
    }

    static <T> T loadProvider(final Class<T> interfaceType, final Supplier<T> defaultProvider) {
        final ServiceLoader<T> loader = ServiceLoader.load(interfaceType);
        if (loader.iterator().hasNext()) {
            return loader.iterator().next();
        }
        return defaultProvider.get();
    }

    static <T extends Annotation> T findQualifier(final Class<T> qualifier, final Annotation[] qualifiers) {
        for (Annotation annotation : qualifiers) {
            if (annotation.annotationType().equals(qualifier)) {
                return qualifier.cast(annotation);
            }
        }
        return null;
    }
}
