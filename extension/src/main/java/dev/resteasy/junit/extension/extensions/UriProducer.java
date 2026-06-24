/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.lang.annotation.Annotation;
import java.net.URI;

import jakarta.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.api.RestResourceProducer;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class UriProducer implements RestResourceProducer {
    @Override
    public boolean canInject(final ExtensionContext context, final Class<?> clazz, final Annotation... qualifiers) {
        return URI.class.isAssignableFrom(clazz);
    }

    @Override
    public Object produce(final ExtensionContext context, final Class<?> clazz, final Annotation... qualifiers) {
        if (URI.class.isAssignableFrom(clazz)) {
            final RequestPath requestPath = Extensions.findQualifier(RequestPath.class, qualifiers);
            @SuppressWarnings("resource")
            final UriBuilder uriBuilder = InstanceManager.getInstance(context)
                    .orElseThrow(() -> new ParameterResolutionException("Could not find associated SeBootstrap instance"))
                    .instance()
                    .configuration()
                    .baseUriBuilder();
            if (requestPath != null) {
                return uriBuilder.path(requestPath.value()).build();
            }
            return uriBuilder.build();
        }
        throw new ParameterResolutionException(String.format("Type %s is not assignable to %s", clazz.getName(), URI.class));
    }
}
