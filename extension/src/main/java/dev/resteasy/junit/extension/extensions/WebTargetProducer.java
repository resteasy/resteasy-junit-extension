/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.lang.annotation.Annotation;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestClientConfig;
import dev.resteasy.junit.extension.api.RestResourceProducer;

/**
 * Allows injecting a {@link WebTarget}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class WebTargetProducer implements RestResourceProducer {

    @Override
    public boolean canInject(final ExtensionContext context, final Class<?> clazz, final Annotation... qualifiers) {
        return WebTarget.class.isAssignableFrom(clazz);
    }

    @Override
    public Object produce(final ExtensionContext context, final Class<?> clazz, final Annotation... qualifiers)
            throws IllegalArgumentException {
        if (WebTarget.class.isAssignableFrom(clazz)) {
            final RestClientConfig restClient = Extensions.findQualifier(RestClientConfig.class, qualifiers);
            @SuppressWarnings("resource")
            final InstanceManager instanceManager = InstanceManager.getInstance(context)
                    .orElseThrow(() -> new ParameterResolutionException("Could not find associated SeBootstrap instance"));
            final Client client = instanceManager.getOrCreateClient(restClient);
            final RequestPath requestPath = Extensions.findQualifier(RequestPath.class, qualifiers);
            final UriBuilder uriBuilder = instanceManager.instance()
                    .configuration()
                    .baseUriBuilder();
            if (requestPath != null) {
                return client.target(uriBuilder).path(requestPath.value());
            }
            return client.target(uriBuilder);
        }
        throw new ParameterResolutionException(String.format("Type %s is not assignable to %s", clazz.getName(), Client.class));
    }

    @Override
    public Scope scope() {
        return Scope.NEW;
    }
}
