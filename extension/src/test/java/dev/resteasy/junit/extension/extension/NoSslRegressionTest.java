/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.extension.resources.EchoResource;

/**
 * Regression test verifying that {@link RestBootstrap @RestBootstrap} without {@link
 * dev.resteasy.junit.extension.annotations.SelfSignedCert @SelfSignedCert} still works over plain HTTP.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(EchoResource.class)
public class NoSslRegressionTest {

    @RestResource
    private Client client;

    @RestResource
    private URI baseUri;

    @Test
    public void serverUsesHttp() {
        Assertions.assertEquals("http", baseUri.getScheme(),
                "Server should use HTTP when @SelfSignedCert is not present");
    }

    @Test
    public void clientWorksOverHttp() {
        final String result = client.target(baseUri)
                .path("/echo")
                .request()
                .post(Entity.text("No SSL"), String.class);
        Assertions.assertEquals("No SSL", result);
    }
}
