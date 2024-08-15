/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import jakarta.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class UriBuilderParameterResolver implements ParameterResolver {
    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return UriBuilder.class.isAssignableFrom(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (UriBuilder.class.isAssignableFrom(parameterContext.getParameter().getType())) {
            return InstanceManager.getInstance(extensionContext)
                    .map(im -> im.instance().configuration().baseUriBuilder())
                    .orElseThrow(() -> new ParameterResolutionException("Failed to lookup URI builder instance."));
        }
        throw new ParameterResolutionException(String.format("Type %s is not assignable to %s",
                parameterContext.getParameter().getType().getName(), UriBuilder.class.getName()));
    }
}
