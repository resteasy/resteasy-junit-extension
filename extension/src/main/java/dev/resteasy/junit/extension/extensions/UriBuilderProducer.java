/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.lang.annotation.Annotation;

import jakarta.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.kohsuke.MetaInfServices;

import dev.resteasy.junit.extension.api.InjectionProducer;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MetaInfServices
public class UriBuilderProducer implements InjectionProducer {
    @Override
    public boolean canInject(final Class<?> clazz) {
        return UriBuilder.class.isAssignableFrom(clazz);
    }

    @Override
    public Object produce(final ExtensionContext context, final Class<?> clazz, final Annotation... qualifiers) {
        if (UriBuilder.class.isAssignableFrom(clazz)) {
            return InstanceManager.getInstance(context)
                    .map(im -> im.instance().configuration().baseUriBuilder())
                    .orElse(null);
        }
        throw new IllegalArgumentException(String.format("Type %s is not assignable to %s", clazz.getName(), UriBuilder.class));
    }
}
