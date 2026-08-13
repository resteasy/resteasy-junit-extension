/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.lang.annotation.Annotation;

import jakarta.ws.rs.SeBootstrap;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import dev.resteasy.junit.extension.api.RestResourceProducer;

/**
 * A producer for injecting a {@link SeBootstrap.Configuration}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ConfigurationProducer implements RestResourceProducer {
    @Override
    public boolean canInject(final ExtensionContext context, final Class<?> clazz, final Annotation... qualifiers) {
        return SeBootstrap.Configuration.class.isAssignableFrom(clazz);
    }

    @Override
    public Object produce(final ExtensionContext context, final Class<?> clazz, final Annotation... qualifiers) {
        return InstanceManager.getInstance(context)
                .map(im -> im.instance().configuration())
                .orElseThrow(() -> new ParameterResolutionException("Could not find associated SeBootstrap instance"));
    }

    @Override
    public Scope scope() {
        return Scope.CLASS;
    }
}
