/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.lang.annotation.Annotation;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.kohsuke.MetaInfServices;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestClientConfig;
import dev.resteasy.junit.extension.api.InjectionProducer;

/**
 * Allows injecting a {@link WebTarget}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MetaInfServices
public class WebTargetProducer implements InjectionProducer {

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
            final Client client = Extensions.findOrCreateClient(context, restClient);
            final RequestPath requestPath = Extensions.findQualifier(RequestPath.class, qualifiers);
            final var uriBuilder = InstanceManager.getInstance(context)
                    .orElseThrow(() -> new RuntimeException("Could not find associated SeBootstrap instance"))
                    .instance()
                    .configuration()
                    .baseUriBuilder();
            if (requestPath != null) {
                return client.target(uriBuilder).path(requestPath.value());
            }
            return client.target(uriBuilder);
        }
        throw new IllegalArgumentException(String.format("Type %s is not assignable to %s", clazz.getName(), Client.class));
    }
}
