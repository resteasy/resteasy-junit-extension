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
 * {@code application()}.
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
     * Test class with neither value() nor application() specified - should fail.
     */
    @RestBootstrap
    public static class NeitherSpecifiedTest {
        @Test
        public void test() {
            Assertions.fail("This test should not run");
        }
    }

    /**
     * Test class with both value() and application() specified - should fail.
     */
    @RestBootstrap(application = TestApplication.class, value = TestResource.class)
    public static class BothSpecifiedTest {
        @Test
        public void test() {
            Assertions.fail("This test should not run");
        }
    }

    /**
     * Test class with only application() specified - should succeed.
     */
    @RestBootstrap(application = TestApplication.class)
    public static class OnlyApplicationSpecifiedTest {

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
     * Test class with only value() specified - should succeed.
     */
    @RestBootstrap(TestResource.class)
    public static class OnlyValueSpecifiedTest {

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
    public void neitherValueNorApplicationSpecified() {
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
                                            "Must define either a Jakarta REST Application via application() or Jakarta REST resource classes via value().");
                });
    }

    @Test
    public void bothValueAndApplicationSpecified() {
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
                                    .contains("Only the value() or application() is allowed to be defined");
                });
    }

    @Test
    public void onlyApplicationSpecified() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(OnlyApplicationSpecifiedTest.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
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
}
