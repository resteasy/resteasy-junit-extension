/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.lang.annotation.Annotation;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.kohsuke.MetaInfServices;

import dev.resteasy.junit.extension.annotations.RestClientConfig;
import dev.resteasy.junit.extension.api.InjectionProducer;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MetaInfServices
public class RestClientProducer implements InjectionProducer {
    @Override
    public boolean canInject(final Class<?> clazz) {
        return Client.class.isAssignableFrom(clazz);
    }

    @Override
    public Object produce(final ExtensionContext context, final Class<?> clazz, final Annotation... qualifiers) {
        if (Client.class.isAssignableFrom(clazz)) {
            final RestClientConfig restClient = InjectionUtil.findQualifier(RestClientConfig.class, qualifiers);
            if (restClient != null) {
                final var factoryType = restClient.value();
                final var factory = InjectionUtil.createProvider(factoryType);
                return factory.getClientBuilder().build();
            }
        } else {
            throw new IllegalArgumentException(String.format("Type %s is not assignable to %s", clazz.getName(), Client.class));
        }
        return ClientBuilder.newClient();
    }
}
