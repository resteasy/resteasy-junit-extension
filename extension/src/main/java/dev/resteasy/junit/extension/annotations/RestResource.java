/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field or constructor parameter for injection by the RESTEasy JUnit extension.
 * <ul>
 * <li>{@link jakarta.ws.rs.SeBootstrap.Configuration}</li>
 * <li>{@link jakarta.ws.rs.client.Client}</li>
 * <li>{@link jakarta.ws.rs.client.WebTarget}</li>
 * <li>{@link jakarta.ws.rs.core.UriBuilder}</li>
 * <li>{@link java.net.URI}</li>
 * </ul>
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * &#x40;RestBootstrap(MyApplication.class)
 * public class MyTest {
 *     &#x40;RestResource
 *     private Client client;
 *
 *     &#x40;RestResource
 *     &#x40;RequestPath("/orders")
 *     private WebTarget ordersTarget;
 *
 *     &#x40;Test
 *     public void testOrder(&#x40;RestResource URI baseUri) {
 *         // baseUri is injected as a parameter
 *     }
 * }
 * </pre>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @since 1.0.0
 */
@Inherited
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface RestResource {
}
