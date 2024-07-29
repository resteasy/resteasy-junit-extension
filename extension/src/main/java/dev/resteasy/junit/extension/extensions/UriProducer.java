/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.lang.annotation.Annotation;
import java.net.URI;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.kohsuke.MetaInfServices;

import dev.resteasy.junit.extension.api.InjectionProducer;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MetaInfServices
public class UriProducer implements InjectionProducer {
    @Override
    public boolean canInject(final ExtensionContext context, final Class<?> clazz, final Annotation... qualifiers) {
        return URI.class.isAssignableFrom(clazz);
    }

    @Override
    public Object produce(final ExtensionContext context, final Class<?> clazz, final Annotation... qualifiers) {
        if (URI.class.isAssignableFrom(clazz)) {
            return InstanceManager.getInstance(context)
                    .map(im -> im.instance().configuration().baseUri())
                    .orElse(null);
        }
        throw new IllegalArgumentException(String.format("Type %s is not assignable to %s", clazz.getName(), URI.class));
    }
}
