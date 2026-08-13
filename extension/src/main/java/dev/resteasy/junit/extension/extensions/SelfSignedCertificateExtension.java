/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.function.Predicate;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;

import dev.resteasy.junit.extension.annotations.SelfSignedCert;
import dev.resteasy.junit.extension.annotations.SslCert;
import dev.resteasy.junit.extension.api.SelfSignedCertificate;

/**
 * An extension used to generate self-signed certificates at runtime and inject them into tests.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @see SelfSignedCertificate
 * @see SelfSignedCert
 * @see SslCert
 */
public class SelfSignedCertificateExtension implements BeforeAllCallback, BeforeEachCallback, ParameterResolver {

    @Override
    public void beforeAll(final ExtensionContext context) {
        final Class<?> testClass = context.getRequiredTestClass();
        final Optional<SelfSignedCert> selfSignedCert = AnnotationSupport.findAnnotation(testClass, SelfSignedCert.class);
        if (selfSignedCert.isEmpty()) {
            return;
        }

        final SelfSignedCertificate cert = DefaultSelfSignedCertificate.getOrCreate(context);
        injectStaticFields(testClass, cert);
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        final SelfSignedCertificate cert = DefaultSelfSignedCertificate.get(context);
        // Do not process the fields if the cert is null
        if (cert != null) {
            context.getRequiredTestInstances().getAllInstances()
                    .forEach(instance -> injectInstanceFields(instance, cert));
        }
    }

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return SelfSignedCertificate.class.isAssignableFrom(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final SelfSignedCertificate cert = DefaultSelfSignedCertificate.get(extensionContext);
        if (cert == null) {
            throw new ParameterResolutionException(
                    "Test class must be annotated with @SelfSignedCert to inject SelfSignedCertificate");
        }
        return cert;

    }

    static void injectInstanceFields(final Object instance, final SelfSignedCertificate value) {
        injectFields(instance, instance.getClass(), (f) -> !Modifier.isStatic(f.getModifiers()), value);
    }

    private static void injectStaticFields(final Class<?> testClass, final SelfSignedCertificate value) {
        injectFields(null, testClass, (f) -> Modifier.isStatic(f.getModifiers()), value);
    }

    private static void injectFields(final Object testInstance, final Class<?> testClass,
            final Predicate<Field> predicate, final SelfSignedCertificate value) {

        AnnotationSupport.findAnnotatedFields(testClass, SslCert.class, predicate).forEach(field -> {
            if (!SelfSignedCertificate.class.isAssignableFrom(field.getType())) {
                throw new ExtensionConfigurationException(
                        String.format("Field '%s' is not of type %s", field, SelfSignedCertificate.class.getName()));
            }
            if (Modifier.isFinal(field.getModifiers())) {
                throw new ExtensionConfigurationException(
                        String.format("Field '%s' cannot be final for injecting a SslCert resource.", field));
            }
            try {
                if (field.trySetAccessible()) {
                    field.set(testInstance, value);
                } else {
                    throw new ExtensionConfigurationException(
                            String.format("Could not make field %s accessible for injection.", field));
                }
            } catch (Exception e) {
                if (e instanceof ExtensionConfigurationException) {
                    throw (ExtensionConfigurationException) e;
                }
                throw new ExtensionConfigurationException(
                        String.format("Could not make field %s accessible for injection.", field), e);
            }
        });
    }
}
