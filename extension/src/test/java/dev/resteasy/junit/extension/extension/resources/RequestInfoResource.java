/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("/info")
public class RequestInfoResource {

    @Context
    private UriInfo uriInfo;

    @GET
    @Path("/request-uri")
    @Produces(MediaType.TEXT_PLAIN)
    public String requestUri() {
        return uriInfo.getRequestUri().toString();
    }
}
