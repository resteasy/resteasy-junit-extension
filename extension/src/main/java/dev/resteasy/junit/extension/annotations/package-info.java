/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Annotations for the RESTEasy JUnit extension.
 * <p>
 * This package contains the primary annotations used to configure and use the RESTEasy JUnit extension for testing
 * Jakarta REST applications.
 * </p>
 *
 * <h2>Primary Annotations</h2>
 * <ul>
 * <li>{@link dev.resteasy.junit.extension.annotations.RestBootstrap @RestBootstrap} - Marks a test class to start a
 * Jakarta REST {@link jakarta.ws.rs.SeBootstrap SeBootstrap} instance</li>
 * <li>{@link dev.resteasy.junit.extension.annotations.RestResource @RestResource} - Marks fields and parameters for
 * injection of REST resources (Client, WebTarget, URI, etc.)</li>
 * </ul>
 *
 * <h2>Qualifier Annotations</h2>
 * <ul>
 * <li>{@link dev.resteasy.junit.extension.annotations.RequestPath @RequestPath} - Qualifies WebTarget or URI injection
 * with a specific path</li>
 * <li>{@link dev.resteasy.junit.extension.annotations.RestClientConfig @RestClientConfig} - Qualifies Client or
 * WebTarget injection with custom configuration</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 *
 * <pre>
 * &#64;RestBootstrap(MyApplication.class)
 * public class MyResourceTest {
 *     &#64;RestResource
 *     private Client client;
 *
 *     &#64;RestResource
 *     &#64;RequestPath("/orders")
 *     private WebTarget ordersTarget;
 *
 *     &#64;Test
 *     public void testGetOrders() {
 *         try (Response response = ordersTarget.request().get()) {
 *             assertEquals(200, response.getStatus());
 *         }
 *     }
 * }
 * </pre>
 *
 * @see dev.resteasy.junit.extension.api
 * @since 1.0.0
 */
package dev.resteasy.junit.extension.annotations;
