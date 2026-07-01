/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Application;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContextException;

import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestClientConfig;
import dev.resteasy.junit.extension.api.ConfigurationProvider;
import dev.resteasy.junit.extension.api.RestClientBuilderProvider;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class InstanceManager implements ExtensionContext.Store.CloseableResource, AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(InstanceManager.class);
    private static final String MANGER_KEY = "InstanceManager";

    private final ReadWriteLock lock;
    private final Class<?> testClass;
    private final RestBootstrap bootstrap;
    private BootstrapHolder holder;

    public InstanceManager(final Class<?> testClass, final RestBootstrap bootstrap) {
        this.testClass = testClass;
        this.bootstrap = bootstrap;
        lock = new ReentrantReadWriteLock();
    }

    @Override
    public void close() {
        LOGGER.debugf("Closing %s", testClass.getName());
        stopInstance();
    }

    static Optional<InstanceManager> getInstance(final ExtensionContext context) {
        final var store = ClassContext.getStore(context);
        return Optional.ofNullable(store.get(MANGER_KEY, InstanceManager.class));
    }

    static InstanceManager getOrCreateInstance(final ExtensionContext context, final Class<?> testClass,
            final RestBootstrap bootstrap) {
        final var store = ClassContext.getStore(context);
        return store.getOrComputeIfAbsent(MANGER_KEY, key -> new InstanceManager(testClass, bootstrap),
                InstanceManager.class);
    }

    SeBootstrap.Instance instance() {
        lock.readLock().lock();
        try {
            if (holder == null) {
                throw new ExtensionContextException("The bootstrap instance has not been started");
            }
            return holder.instance;
        } finally {
            lock.readLock().unlock();
        }
    }

    void startInstance(final ExtensionContext context) throws ExecutionException, InterruptedException {
        lock.readLock().lock();
        try {
            if (holder != null) {
                return;
            }
        } finally {
            lock.readLock().unlock();
        }
        lock.writeLock().lock();
        try {
            holder = new BootstrapHolder();
            final Class<? extends ConfigurationProvider> factoryType = bootstrap.configFactory();
            final ConfigurationProvider factory = createProvider(factoryType, ConfigurationProvider.class,
                    DefaultConfigurationProvider::new);
            final SeBootstrap.Configuration configuration = factory.getConfiguration(context);
            final CompletionStage<SeBootstrap.Instance> stage;
            if (bootstrap.application() == Application.class) {
                final Set<Class<?>> resources = Set.of(bootstrap.value());
                stage = SeBootstrap.start(new Application() {
                    @Override
                    public Set<Class<?>> getClasses() {
                        return resources;
                    }
                }, configuration);
            } else {
                stage = SeBootstrap.start(bootstrap.application(), configuration);
            }
            holder.instance = stage.toCompletableFuture()
                    .get(bootstrap.timeout(), bootstrap.timeoutUnit());
        } catch (TimeoutException e) {
            throw new ExtensionContextException(String.format("Failed to start the SeBootstrap instance in %d %s",
                    bootstrap.timeout(), bootstrap.timeoutUnit()
                            .toChronoUnit()),
                    e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    void stopInstance() {
        if (holder != null) {
            lock.writeLock().lock();
            try {
                holder.close();
                if (holder.instance != null) {
                    holder.instance.stop()
                            .toCompletableFuture()
                            .get(bootstrap.timeout(), bootstrap.timeoutUnit());
                }
            } catch (TimeoutException e) {
                throw new ExtensionContextException(String.format("Failed to stop the SeBootstrap instance in %d %s",
                        bootstrap.timeout(), bootstrap.timeoutUnit()
                                .toChronoUnit()),
                        e);
            } catch (InterruptedException ignore) {
                // Ignore the interrupted exception
            } catch (ExecutionException e) {
                throw new ExtensionContextException("Failed waiting for the server to stop", e);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * Gets or creates a Client for this SeBootstrap instance.
     *
     * @param restClientConfig optional configuration for the client
     *
     * @return the client instance
     */
    Client getOrCreateClient(final RestClientConfig restClientConfig) {
        lock.readLock().lock();
        try {
            if (holder == null) {
                throw new ExtensionContextException("The bootstrap instance has not been started");
            }
            final String key = clientKey(restClientConfig);
            return holder.clients.computeIfAbsent(key, k -> createClient(restClientConfig));
        } finally {
            lock.readLock().unlock();
        }
    }

    private String clientKey(final RestClientConfig restClientConfig) {
        if (restClientConfig == null) {
            return "default";
        }
        return "config-" + System.identityHashCode(restClientConfig.value());
    }

    private Client createClient(final RestClientConfig restClientConfig) {
        if (restClientConfig == null) {
            return loadProvider(RestClientBuilderProvider.class, () -> ClientBuilder::newBuilder).getClientBuilder()
                    .build();
        }
        final var factoryType = restClientConfig.value();
        final var factory = createProvider(factoryType, RestClientBuilderProvider.class,
                () -> ClientBuilder::newBuilder);
        final var builder = factory.getClientBuilder();
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static <T> T createProvider(final Class<? extends T> factoryType, final Class<T> interfaceType,
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

    private static <T> T loadProvider(final Class<T> interfaceType, final Supplier<T> defaultProvider) {
        final ServiceLoader<T> loader = ServiceLoader.load(interfaceType);
        if (loader.iterator().hasNext()) {
            return loader.iterator().next();
        }
        return defaultProvider.get();
    }

    private static class BootstrapHolder implements AutoCloseable {
        private SeBootstrap.Instance instance;
        private final Map<String, Client> clients = new ConcurrentHashMap<>();

        @Override
        public void close() {
            for (Map.Entry<String, Client> entry : clients.entrySet()) {
                final Client client = entry.getValue();
                if (clients.remove(entry.getKey(), client)) {
                    try {
                        client.close();
                    } catch (Exception e) {
                        LOGGER.debugf(e, "Failed to close client %s", client);
                    }
                }
            }
        }
    }
}
