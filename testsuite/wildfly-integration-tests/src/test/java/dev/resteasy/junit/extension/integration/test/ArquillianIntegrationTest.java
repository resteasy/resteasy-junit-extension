/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.integration.test;

import java.net.URI;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import dev.resteasy.junit.extension.annotations.RestBootstrap;

/**
 * Tests that Arquillian works with WildFly and the SeBootstrap. An instance of SeBootstrap is started and listens on
 * port 8085. The WildFly deployment sends a request to the SeBootstrap instance and checks the response.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RestBootstrap(ArquillianIntegrationTest.SeApplication.class)
@ExtendWith(ArquillianExtension.class)
@ApplicationScoped
public class ArquillianIntegrationTest {
    private static final String SE_URI = "http://localhost:" + TestConfigurationProvider.PORT + "/se/echo";

    // Injected by Arquillian
    @ArquillianResource
    private URI uri;

    // Injected for client usage by the RESTEasy JUnit 5 extension when @RunAsClient used. Injected by CDI when used
    // in the container.
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private Client client;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(ServerApplication.class, ClientResource.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * Checks the injected client can communicate with the SeBootstrap instance when injected by RESTEasy.
     */
    @Test
    @RunAsClient
    public void checkTestClient() {
        final String response = client.target(UriBuilder.fromUri(uri).path("server/client"))
                .request()
                .get(String.class);
        Assertions.assertEquals("Hello From WildFly", response);
    }

    /**
     * Checks the injected client can communicate with the SeBootstrap instance from within a WildFly test deployment.
     */
    @Test
    public void checkInContainerClient() {
        final String response = client.target(SE_URI)
                .request()
                .post(Entity.text("In Container Test"), String.class);
        Assertions.assertEquals("In Container Test", response);
    }

    @ApplicationPath("se")
    public static class SeApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            return Set.of(EchoResource.class);
        }
    }

    @Path("echo")
    @RequestScoped
    public static class EchoResource {
        @POST
        public String echo(final String input) {
            return input;
        }
    }

    @ApplicationPath("server")
    public static class ServerApplication extends Application {
    }

    @Path("client")
    @RequestScoped
    public static class ClientResource {
        @Inject
        private Client client;

        @GET
        public String clientRequest() {
            return client.target(SE_URI)
                    .request()
                    .post(Entity.text("Hello From WildFly"), String.class);
        }
    }
}
