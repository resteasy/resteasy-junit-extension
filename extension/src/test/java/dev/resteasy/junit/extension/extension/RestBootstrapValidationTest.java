/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineTestKit;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

/**
 * Tests validation of {@link RestBootstrap} annotation to ensure mutual exclusivity of {@code value()} and
 * {@code resources()}.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class RestBootstrapValidationTest {

    @Path("/test")
    public static class TestResource {
        @GET
        public String get() {
            return "test";
        }
    }

    @ApplicationPath("/test-api")
    public static class TestApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            return Set.of(TestResource.class);
        }
    }

    /**
     * Test class with neither value() nor resources() specified - should fail.
     */
    @RestBootstrap
    public static class NeitherSpecifiedTest {
        @Test
        public void test() {
            Assertions.fail("This test should not run");
        }
    }

    /**
     * Test class with both value() and resources() specified - should fail.
     */
    @RestBootstrap(value = TestApplication.class, resources = { TestResource.class })
    public static class BothSpecifiedTest {
        @Test
        public void test() {
            Assertions.fail("This test should not run");
        }
    }

    /**
     * Test class with only value() specified - should succeed.
     */
    @RestBootstrap(TestApplication.class)
    public static class OnlyValueSpecifiedTest {

        @RestResource
        @RequestPath("test-api/test")
        private WebTarget target;

        @Test
        public void test() {
            try (Response response = target.request().get()) {
                Assertions.assertEquals(200, response.getStatus(),
                        () -> String.format("Response code %d%n%s", response.getStatus(), response.readEntity(String.class)));
                Assertions.assertEquals("test", response.readEntity(String.class));
            }
        }
    }

    /**
     * Test class with only resources() specified - should succeed.
     */
    @RestBootstrap(resources = { TestResource.class })
    public static class OnlyResourcesSpecifiedTest {

        @RestResource
        @RequestPath("test")
        private WebTarget target;

        @Test
        public void test() {
            try (Response response = target.request().get()) {
                Assertions.assertEquals(200, response.getStatus(),
                        () -> String.format("Response code %d%n%s", response.getStatus(), response.readEntity(String.class)));
                Assertions.assertEquals("test", response.readEntity(String.class));
            }
        }
    }

    @Test
    public void neitherValueNorResourcesSpecified() {
        final var results = EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(NeitherSpecifiedTest.class))
                .execute();

        // Container events: should have 2 started (engine + class), 1 succeeded (engine), 1 failed (class)
        results.containerEvents()
                .assertStatistics(stats -> stats.started(2).succeeded(1).failed(1));

        // Verify the failure message
        results.containerEvents()
                .failed()
                .assertThatEvents()
                .hasSize(1)
                .anyMatch(event -> {
                    final var throwable = event.getPayload(TestExecutionResult.class)
                            .flatMap(TestExecutionResult::getThrowable);
                    return throwable.isPresent()
                            && throwable.get().getMessage()
                                    .contains(
                                            "Must define either a Jakarta REST Application in the value or Jakarta REST resources in the resources");
                });
    }

    @Test
    public void bothValueAndResourcesSpecified() {
        final var results = EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(BothSpecifiedTest.class))
                .execute();

        // Container events: should have 2 started (engine + class), 1 succeeded (engine), 1 failed (class)
        results.containerEvents()
                .assertStatistics(stats -> stats.started(2).succeeded(1).failed(1));

        // Verify the failure message
        results.containerEvents()
                .failed()
                .assertThatEvents()
                .hasSize(1)
                .anyMatch(event -> {
                    final var throwable = event.getPayload(TestExecutionResult.class)
                            .flatMap(TestExecutionResult::getThrowable);
                    return throwable.isPresent()
                            && throwable.get().getMessage()
                                    .contains("Only the value() or resources() is allowed to be defined");
                });
    }

    @Test
    public void onlyValueSpecified() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(OnlyValueSpecifiedTest.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
    }

    @Test
    public void onlyResourcesSpecified() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(OnlyResourcesSpecifiedTest.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
    }
}
