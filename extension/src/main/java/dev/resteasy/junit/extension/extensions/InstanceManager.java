/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jakarta.ws.rs.SeBootstrap;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.SearchOption;

import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.api.ConfigurationProvider;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class InstanceManager implements ExtensionContext.Store.CloseableResource {
    private static final String MANGER_KEY = "InstanceManager";

    private final ReadWriteLock lock;
    private final Class<?> testClass;
    private BootstrapHolder holder;

    public InstanceManager(final Class<?> testClass) {
        this.testClass = testClass;
        lock = new ReentrantReadWriteLock();
    }

    @Override
    public void close() {
        stopInstance();
    }

    static Optional<InstanceManager> getInstance(final ExtensionContext context) {
        final var store = getStore(context);
        return Optional.ofNullable(store.get(MANGER_KEY, InstanceManager.class));
    }

    static Optional<InstanceManager> removeInstance(final ExtensionContext context) {
        final var store = getStore(context);
        return Optional.ofNullable(store.remove(MANGER_KEY, InstanceManager.class));
    }

    static InstanceManager getOrCreateInstance(final ExtensionContext context) {
        final var store = getStore(context);
        return store.getOrComputeIfAbsent(MANGER_KEY, key -> new InstanceManager(context.getRequiredTestClass()),
                InstanceManager.class);
    }

    RestBootstrap bootstrap() {
        lock.readLock().lock();
        try {
            if (holder == null) {
                throw new IllegalStateException("The bootstrap instance has not been started");
            }
            return holder.bootstrap;
        } finally {
            lock.readLock().unlock();
        }
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

    void startInstance() throws ExecutionException, InterruptedException {
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
            final Optional<RestBootstrap> bootstrap = AnnotationSupport.findAnnotation(testClass, RestBootstrap.class,
                    SearchOption.INCLUDE_ENCLOSING_CLASSES);
            if (bootstrap.isEmpty()) {
                return;
            }
            holder = new BootstrapHolder();
            holder.bootstrap = bootstrap.get();
            final Class<? extends ConfigurationProvider> factoryType = holder.bootstrap.configFactory();
            final ConfigurationProvider factory = InjectionUtil.createProvider(factoryType);
            holder.instance = SeBootstrap.start(holder.bootstrap.value(), factory.getConfiguration())
                    .toCompletableFuture()
                    .get(holder.bootstrap.timeout(), holder.bootstrap.timoutUnit());
        } catch (TimeoutException e) {
            throw new AssertionError(String.format("Failed to start the SeBootstrap instance in %d %s",
                    holder.bootstrap.timeout(), holder.bootstrap.timoutUnit()
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
                if (holder.instance != null) {
                    holder.instance.stop()
                            .toCompletableFuture()
                            .get(holder.bootstrap.timeout(), holder.bootstrap.timoutUnit());
                }
            } catch (TimeoutException e) {
                throw new AssertionError(String.format("Failed to stop the SeBootstrap instance in %d %s",
                        holder.bootstrap.timeout(), holder.bootstrap.timoutUnit()
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

    private static ExtensionContext.Store getStore(final ExtensionContext context) {
        return context.getRoot().getStore(ExtensionContext.Namespace.create("SeBootstrap.Store"));
    }

    private static class BootstrapHolder {
        private RestBootstrap bootstrap;
        private SeBootstrap.Instance instance;
    }
}
