/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.util.Optional;

import jakarta.ws.rs.core.Application;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import dev.resteasy.junit.extension.annotations.RestBootstrap;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class SeBootstrapExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        final Optional<RestBootstrap> bootstrap = AnnotationSupport.findAnnotation(testClass, RestBootstrap.class);
        if (bootstrap.isEmpty()) {
            return;
        }
        final RestBootstrap restBootstrap = bootstrap.get();
        // Validate the RestBootstrap annotation. Only the value() or the application() can be defined, but not both
        if (restBootstrap.application() == Application.class && restBootstrap.value().length == 0) {
            throw new ExtensionConfigurationException(
                    "Must define either a Jakarta REST Application via application() or Jakarta REST resource classes via value().");
        }
        if (restBootstrap.application() != Application.class && restBootstrap.value().length > 0) {
            throw new ExtensionConfigurationException("Only the value() or application() is allowed to be defined.");
        }
        InstanceManager.getOrCreateInstance(context, testClass, restBootstrap).startInstance(context);
    }
}
