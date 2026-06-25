/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.extension.resources.TestApplication;

/**
 * Tests WebTarget injection with various path configurations.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(TestApplication.class)
public class WebTargetTest {

    @RestResource
    @RequestPath("/echo")
    private WebTarget echoTarget;

    @RestResource
    @RequestPath("echo")
    private WebTarget echoTargetNoSlash;

    @Test
    public void webTargetWithLeadingSlash() {
        try (Response response = echoTarget.request().post(Entity.text("leading slash"))) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("leading slash", response.readEntity(String.class));
        }
    }

    @Test
    public void webTargetWithoutLeadingSlash() {
        try (Response response = echoTargetNoSlash.request().post(Entity.text("no slash"))) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("no slash", response.readEntity(String.class));
        }
    }

    @Test
    public void webTargetParameter(@RestResource @RequestPath("/echo") WebTarget target) {
        try (Response response = target.request().post(Entity.text("parameter"))) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("parameter", response.readEntity(String.class));
        }
    }

    @Test
    public void multipleWebTargets(@RestResource @RequestPath("/echo") WebTarget target1,
            @RestResource @RequestPath("echo") WebTarget target2) {
        try (Response response1 = target1.request().post(Entity.text("first"));
                Response response2 = target2.request().post(Entity.text("second"))) {
            Assertions.assertEquals(200, response1.getStatus());
            Assertions.assertEquals("first", response1.readEntity(String.class));
            Assertions.assertEquals(200, response2.getStatus());
            Assertions.assertEquals("second", response2.readEntity(String.class));
        }
    }

    @Test
    public void webTargetUri() {
        // Verify the target points to the expected path
        final String uri = echoTarget.getUri().toString();
        Assertions.assertTrue(uri.endsWith("/echo"), () -> String.format("Expected URI to end with /echo but was: %s", uri));
    }
}
