/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class SeBootstrapExtension implements BeforeAllCallback, AfterAllCallback {
    private final Lock lock = new ReentrantLock();

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        lock.lock();
        try {
            InstanceManager.getOrCreateInstance(context).startInstance();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        lock.lock();
        try {
            InstanceManager.removeInstance(context).ifPresent(InstanceManager::stopInstance);
        } finally {
            lock.unlock();
        }
    }
}
