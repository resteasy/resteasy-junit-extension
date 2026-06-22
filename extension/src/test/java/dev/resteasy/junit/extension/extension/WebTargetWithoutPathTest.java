/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import java.net.URI;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.extension.resources.TestApplication;

/**
 * Tests WebTarget injection without @RequestPath qualifier.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(TestApplication.class)
public class WebTargetWithoutPathTest {

    @RestResource
    private WebTarget webTarget;

    @Test
    public void webTargetWithoutRequestPath() {
        Assertions.assertNotNull(webTarget, "WebTarget should be injected even without @RequestPath");

        // The WebTarget should point to the base URI
        final URI targetUri = webTarget.getUri();
        Assertions.assertNotNull(targetUri, "WebTarget should have a URI");

        // Should be able to add a path and make a request
        try (Response response = webTarget.path("/echo").request().post(Entity.text("no path annotation"))) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("no path annotation", response.readEntity(String.class));
        }
    }

    @Test
    public void webTargetParameterWithoutRequestPath(@RestResource final WebTarget target) {
        Assertions.assertNotNull(target, "WebTarget parameter should be injected without @RequestPath");

        // Should point to base URI
        final URI targetUri = target.getUri();
        Assertions.assertNotNull(targetUri, "WebTarget should have a URI");

        try (Response response = target.path("/echo").request().post(Entity.text("parameter no path"))) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("parameter no path", response.readEntity(String.class));
        }
    }

    @Test
    public void webTargetBaseUri(@RestResource final WebTarget target, @RestResource final URI baseUri) {
        // WebTarget without @RequestPath should point to the same URI as the injected base URI
        Assertions.assertEquals(baseUri, target.getUri(),
                "WebTarget without @RequestPath should point to base URI");
    }
}
