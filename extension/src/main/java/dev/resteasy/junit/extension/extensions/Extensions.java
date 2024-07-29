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
import java.util.Objects;
import java.util.UUID;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import org.junit.jupiter.api.extension.ExtensionContext;

import dev.resteasy.junit.extension.annotations.RestClientConfig;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
// TODO (jrp) possibly rename this
class Extensions {

    private static final ExtensionContext.Namespace CLIENT_NAMESPACE = ExtensionContext.Namespace.create(Client.class);
    private static final String CLIENT_KEY = "client";

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

    static Client findOrCreateClient(final ExtensionContext context, final RestClientConfig restClientConfig) {
        final var store = context.getStore(CLIENT_NAMESPACE);
        final String key = CLIENT_KEY + "-" + UUID.randomUUID();
        if (restClientConfig == null) {
            return store.getOrComputeIfAbsent(key, s -> new ClientStore(CLIENT_KEY, ClientBuilder.newClient()),
                    ClientStore.class).client;
        }
        final var factoryType = restClientConfig.value();
        final var factory = Extensions.createProvider(factoryType);
        final var builder = factory.getClientBuilder();
        return store.getOrComputeIfAbsent(key, s -> new ClientStore(CLIENT_KEY, builder.build()), ClientStore.class).client;
    }

    private static class ClientStore implements ExtensionContext.Store.CloseableResource {
        private final String key;
        private final Client client;

        private ClientStore(final String key, final Client client) {
            this.key = key;
            this.client = client;
        }

        @Override
        public void close() {
            client.close();
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ClientStore)) {
                return false;
            }
            final ClientStore other = (ClientStore) obj;
            return Objects.equals(key, other.key);
        }

        @Override
        public String toString() {
            return "ClientStore[" + "key='" + key + "client=" + client + ']';
        }
    }
}
