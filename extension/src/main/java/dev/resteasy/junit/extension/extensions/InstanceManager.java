/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Application;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;

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
                throw new IllegalStateException("The bootstrap instance has not been started");
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
            final ConfigurationProvider factory = Extensions.createProvider(factoryType, ConfigurationProvider.class,
                    DefaultConfigurationProvider::new);
            final SeBootstrap.Configuration configuration = factory.getConfiguration(context);
            final CompletionStage<SeBootstrap.Instance> stage;
            if (bootstrap.value() == Application.class) {
                final Set<Class<?>> resources = Set.of(bootstrap.resources());
                stage = SeBootstrap.start(new Application() {
                    @Override
                    public Set<Class<?>> getClasses() {
                        return resources;
                    }
                }, configuration);
            } else {
                stage = SeBootstrap.start(bootstrap.value(), configuration);
            }
            holder.instance = stage.toCompletableFuture()
                    .get(bootstrap.timeout(), bootstrap.timeoutUnit());
        } catch (TimeoutException e) {
            throw new AssertionError(String.format("Failed to start the SeBootstrap instance in %d %s",
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
                throw new AssertionError(String.format("Failed to stop the SeBootstrap instance in %d %s",
                        bootstrap.timeout(), bootstrap.timeoutUnit()
                                .toChronoUnit()),
                        e);
            } catch (InterruptedException ignore) {
                // Ignore the interrupted exception
            } catch (ExecutionException e) {
                throw new AssertionError("Failed waiting for the server to stop", e);
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
                throw new IllegalStateException("The bootstrap instance has not been started");
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
            return Extensions.loadProvider(RestClientBuilderProvider.class, () -> ClientBuilder::newBuilder).getClientBuilder()
                    .build();
        }
        final var factoryType = restClientConfig.value();
        final var factory = Extensions.createProvider(factoryType, RestClientBuilderProvider.class,
                () -> ClientBuilder::newBuilder);
        final var builder = factory.getClientBuilder();
        return builder.build();
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
