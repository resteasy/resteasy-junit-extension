/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.lang.annotation.Annotation;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class Extensions {

    static <T extends Annotation> T findQualifier(final Class<T> qualifier, final Annotation[] qualifiers) {
        for (Annotation annotation : qualifiers) {
            if (annotation.annotationType().equals(qualifier)) {
                return qualifier.cast(annotation);
            }
        }
        return null;
    }
}
