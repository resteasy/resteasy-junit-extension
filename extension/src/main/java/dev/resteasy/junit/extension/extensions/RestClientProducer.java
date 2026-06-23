/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.lang.annotation.Annotation;

import jakarta.ws.rs.client.Client;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.kohsuke.MetaInfServices;

import dev.resteasy.junit.extension.annotations.RestClientConfig;
import dev.resteasy.junit.extension.api.RestResourceProducer;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MetaInfServices
public class RestClientProducer implements RestResourceProducer {
    @Override
    public boolean canInject(final ExtensionContext context, final Class<?> clazz, final Annotation... qualifiers) {
        return Client.class.isAssignableFrom(clazz);
    }

    @Override
    public Object produce(final ExtensionContext context, final Class<?> clazz, final Annotation... qualifiers) {
        if (Client.class.isAssignableFrom(clazz)) {
            final RestClientConfig restClient = Extensions.findQualifier(RestClientConfig.class, qualifiers);
            // Get the InstanceManager for this test class and ask it for a Client
            @SuppressWarnings("resource")
            final InstanceManager instanceManager = InstanceManager.getInstance(context)
                    .orElseThrow(() -> new ParameterResolutionException("No SeBootstrap instance available"));
            return instanceManager.getOrCreateClient(restClient);
        }
        throw new ParameterResolutionException(String.format("Type %s is not assignable to %s", clazz.getName(), Client.class));
    }
}
