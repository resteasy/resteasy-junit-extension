/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.testkit.engine.EngineTestKit;

import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.api.RestResourceProducer;
import dev.resteasy.junit.extension.extension.resources.TestApplication;

/**
 * Tests {@link RestResourceProducer.Scope} behavior for CLASS vs DEFAULT scopes.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class ResourceScopeTest {

    /**
     * Tracks resource lifecycle events for testing.
     */
    public static class TrackableResource implements AutoCloseable {
        private static final AtomicInteger instanceCounter = new AtomicInteger(0);
        private static final List<String> events = new ArrayList<>();

        private final int id;
        private final String name;

        public TrackableResource(final String name) {
            this.id = instanceCounter.incrementAndGet();
            this.name = name;
            events.add("created:" + name + ":" + id);
        }

        @Override
        public void close() {
            events.add("closed:" + name + ":" + id);
        }

        public int id() {
            return id;
        }

        public static void reset() {
            instanceCounter.set(0);
            events.clear();
        }

        public static List<String> getEvents() {
            return new ArrayList<>(events);
        }
    }

    /**
     * Producer with DEFAULT scope - method parameters get method-scoped cleanup.
     */
    public static class DefaultScopeProducer implements RestResourceProducer {
        @Override
        public boolean canInject(final ExtensionContext context, final Class<?> clazz, final Annotation... qualifiers) {
            return TrackableResource.class.equals(clazz) && hasDefaultScopeMarker(qualifiers);
        }

        @Override
        public Object produce(final ExtensionContext context, final Class<?> clazz, final Annotation... qualifiers) {
            return new TrackableResource("default-scope");
        }

        private boolean hasDefaultScopeMarker(final Annotation[] qualifiers) {
            return Stream.of(qualifiers)
                    .anyMatch((a) -> a instanceof DefaultScopeMarker);
        }
    }

    /**
     * Producer with CLASS scope - all injections get class-scoped cleanup.
     */
    public static class ClassScopeProducer implements RestResourceProducer {
        @Override
        public boolean canInject(final ExtensionContext context, final Class<?> clazz, final Annotation... qualifiers) {
            return TrackableResource.class.equals(clazz) && hasClassScopeMarker(qualifiers);
        }

        @Override
        public Object produce(final ExtensionContext context, final Class<?> clazz, final Annotation... qualifiers) {
            return new TrackableResource("class-scope");
        }

        @Override
        public Scope scope() {
            return Scope.CLASS;
        }

        private boolean hasClassScopeMarker(final Annotation[] qualifiers) {
            return Stream.of(qualifiers)
                    .anyMatch((a) -> a instanceof ClassScopeMarker);
        }
    }

    /**
     * Marker annotation for DEFAULT scope producer.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.PARAMETER })
    public @interface DefaultScopeMarker {
    }

    /**
     * Marker annotation for CLASS scope producer.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.PARAMETER })
    public @interface ClassScopeMarker {
    }

    /**
     * Test class using DEFAULT-scoped resources.
     */
    @RestBootstrap(application = TestApplication.class)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @SuppressWarnings("NewClassNamingConvention")
    public static class DefaultScopeTestClass {

        @RestResource
        @DefaultScopeMarker
        private TrackableResource fieldResource;

        private int test1ParamId = -1;

        @Test
        @Order(1)
        public void test1(@RestResource @DefaultScopeMarker TrackableResource paramResource) {
            Assertions.assertNotNull(fieldResource, "Field resource should be injected");
            Assertions.assertNotNull(paramResource, "Parameter resource should be injected");
            Assertions.assertNotSame(fieldResource, paramResource,
                    "The instance field and the parameter resource should not be the same instance.");
            test1ParamId = paramResource.id();
        }

        @Test
        @Order(2)
        public void test2(@RestResource @DefaultScopeMarker TrackableResource paramResource) {
            Assertions.assertNotNull(fieldResource, "Field resource should be injected");
            Assertions.assertNotNull(paramResource, "Parameter resource should be injected");
            Assertions.assertNotSame(fieldResource, paramResource,
                    "The instance field and the parameter resource should not be the same instance.");
            final int test2ParamId = paramResource.id();

            // With DEFAULT scope, parameters should get new instances per method
            Assertions.assertNotEquals(test1ParamId, test2ParamId,
                    "DEFAULT scope parameters should have different instances across methods");
        }
    }

    /**
     * Test class using CLASS-scoped resources.
     */
    @RestBootstrap(application = TestApplication.class)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @SuppressWarnings("NewClassNamingConvention")
    public static class ClassScopeTestClass {

        @RestResource
        @ClassScopeMarker
        private TrackableResource fieldResource;

        private static int test1ParamId = -1;

        @Test
        @Order(1)
        public void test1(@RestResource @ClassScopeMarker final TrackableResource paramResource) {
            Assertions.assertNotNull(fieldResource, "Field resource should be injected");
            Assertions.assertNotNull(paramResource, "Parameter resource should be injected");
            Assertions.assertSame(fieldResource, paramResource,
                    "The instance field and the parameter resource should be the same instance.");
            test1ParamId = paramResource.id();
        }

        @Test
        @Order(2)
        public void test2(@RestResource @ClassScopeMarker final TrackableResource paramResource) {
            Assertions.assertNotNull(fieldResource, "Field resource should be injected");
            Assertions.assertNotNull(paramResource, "Parameter resource should be injected");
            Assertions.assertSame(fieldResource, paramResource,
                    "The instance field and the parameter resource should be the same instance.");
            final int test2ParamId = paramResource.id();

            // With CLASS scope, parameters should reuse the same instance
            Assertions.assertEquals(test1ParamId, test2ParamId,
                    "CLASS scope parameters should have the same instance across methods");
        }
    }

    @Test
    public void testDefaultScopeLifecycle() {
        TrackableResource.reset();

        EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(DefaultScopeTestClass.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(2).succeeded(2).failed(0));

        final List<String> events = TrackableResource.getEvents();

        // Field resource: created once, closed once (class scope)
        final long fieldCreated = events.stream().filter(e -> e.startsWith("created:default-scope")).count();
        final long fieldClosed = events.stream().filter(e -> e.startsWith("closed:default-scope")).count();

        // Should have field + 2 parameters created, and all closed
        Assertions.assertEquals(3, fieldCreated, "Should create 1 field + 2 parameter instances");
        Assertions.assertEquals(3, fieldClosed, "All resources should be closed");

        // Parameters should be closed before the field (method scope closes before class scope)
        int firstParamCloseIndex = -1;
        int lastParamCloseIndex = -1;
        int fieldCloseIndex = -1;

        for (int i = 0; i < events.size(); i++) {
            String event = events.get(i);
            if (event.startsWith("closed:default-scope")) {
                int id = Integer.parseInt(event.split(":")[2]);
                if (id == 1) {
                    fieldCloseIndex = i;
                } else {
                    if (firstParamCloseIndex == -1) {
                        firstParamCloseIndex = i;
                    }
                    lastParamCloseIndex = i;
                }
            }
        }

        Assertions.assertTrue(lastParamCloseIndex < fieldCloseIndex,
                "Method-scoped parameters should be closed before class-scoped field");
    }

    @Test
    public void testClassScopeLifecycle() {
        TrackableResource.reset();

        EngineTestKit
                .engine("junit-jupiter")
                .selectors(DiscoverySelectors.selectClass(ClassScopeTestClass.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(2).succeeded(2).failed(0));

        final List<String> events = TrackableResource.getEvents();

        // With CLASS scope, only 2 instances should be created: 1 field + 1 parameter (reused)
        final long created = events.stream().filter(e -> e.startsWith("created:class-scope")).count();
        final long closed = events.stream().filter(e -> e.startsWith("closed:class-scope")).count();

        Assertions.assertEquals(1, created,
                "There should have only been one instance created and shared for the instance field and parameter.");
        Assertions.assertEquals(1, closed, "The resource should have been closed.");
    }
}
