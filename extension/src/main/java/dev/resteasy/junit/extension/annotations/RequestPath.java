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
import java.net.URI;

import jakarta.ws.rs.SeBootstrap;

/**
 * Qualifies the injection point for a {@link jakarta.ws.rs.client.WebTarget} or {@link URI} with a relative path
 * with a prefix of {@link SeBootstrap.Configuration#baseUriBuilder()}.
 *
 * <pre>
 * &#x40;RestBootstrap(OrderApplication.class)
 * public class OrderTest {
 *     &#x40;RestResource
 *     &#x40;RequestPath("/orders")
 *     private WebTarget ordersTarget;
 *
 *     &#x40;RestResource
 *     &#x40;RequestPath("/orders")
 *     private URI ordersUri;
 *
 *     &#x40;Test
 *     public void listOrders() throws Exception {
 *         try (Response response = ordersTarget.request().get()) {
 *             Assertions.assertEquals(200, response.getStatus(),
 *                     () -> String.format("Failed to get orders: %s", response.readEntity(String.class)));
 *         }
 *     }
 *
 *     &#x40;Test
 *     public void getOrdersUri(&#x40;RestResource &#x40;RequestPath("/orders/123") URI orderUri) {
 *         Assertions.assertTrue(orderUri.toString().endsWith("/orders/123"));
 *     }
 * }
 * </pre>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Inherited
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestPath {

    /**
     * The relative path the {@link jakarta.ws.rs.client.Client#target(URI)} should use based on the
     * {@link SeBootstrap.Configuration#baseUriBuilder()} path.
     *
     * @return the relative path
     */
    String value();
}
