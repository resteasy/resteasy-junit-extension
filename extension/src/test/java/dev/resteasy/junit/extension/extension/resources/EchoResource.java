/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension.resources;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("/echo")
public class EchoResource {
    @POST
    public String echo(String text) {
        return text;
    }
}
