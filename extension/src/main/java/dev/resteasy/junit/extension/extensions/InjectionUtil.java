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

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class InjectionUtil {

    @SuppressWarnings("unchecked")
    static <T> T createProvider(final Class<? extends T> factoryType) {
        if (factoryType.isInterface()) {
            final Class<?>[] interfaces = { factoryType };
            return (T) Proxy.newProxyInstance(factoryType.getClassLoader(), interfaces,
                    (proxy, method, args) -> MethodHandles
                            .privateLookupIn(factoryType, MethodHandles.lookup())
                            .in(factoryType)
                            .unreflectSpecial(method, factoryType)
                            .bindTo(proxy)
                            .invokeWithArguments());
        } else {
            try {
                final Constructor<? extends T> constructor = factoryType.getConstructor();
                return constructor.newInstance();
            } catch (InvocationTargetException | InstantiationException e) {
                throw new RuntimeException(String.format("Failed to create provider %s", factoryType.getName()), e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(
                        String.format("Failed to find no-arg constructor for type %s", factoryType.getName()), e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(String.format("Constructor for %s is not public.", factoryType.getName()), e);
            }
        }
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
