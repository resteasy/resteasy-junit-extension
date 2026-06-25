/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extension;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.api.RestResourceProducer;
import dev.resteasy.junit.extension.extension.resources.TestApplication;

/**
 * Demonstrates a realistic use case for DEFAULT (method-scoped) resources. Temporary file that should be cleaned up
 * after each test method.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(TestApplication.class)
public class MethodScopedResourceTest {
    private static final Path TEMP_FILE = Path.of(System.getProperty("java.io.tmpdir"), "test.tmp");

    /**
     * A temporary file resource that gets deleted when closed.
     */
    public static class TempFile implements AutoCloseable {

        private final Path path;

        public TempFile(final Path path) throws IOException {
            this.path = path;
            Files.createFile(path);
        }

        Path getPath() {
            return path;
        }

        @Override
        public void close() throws IOException {
            Assertions.assertTrue(Files.deleteIfExists(path), () -> String.format("Failed to delete file %s", path));
        }
    }

    /**
     * Producer for temporary files with DEFAULT scope - method parameters get cleaned up per-method.
     */
    public static class TempFileProducer implements RestResourceProducer {

        @Override
        public boolean canInject(final ExtensionContext context, final Class<?> clazz, final Annotation... qualifiers) {
            return TempFile.class.equals(clazz);
        }

        @Override
        public Object produce(final ExtensionContext context, final Class<?> clazz, final Annotation... qualifiers) {
            try {
                return new TempFile(TEMP_FILE);
            } catch (IOException e) {
                throw new AssertionError("Failed to create temp file", e);
            }
        }
    }

    @BeforeEach
    public void createFile() {
        Assertions.assertTrue(Files.notExists(TEMP_FILE), () -> String
                .format("File %s already exists which indicates it was not deleted when the TempFile was closed.", TEMP_FILE));
    }

    @Test
    public void testMethod1(@RestResource final TempFile tempFile) throws IOException {
        Assertions.assertNotNull(tempFile, "Temp file should be injected");
        final Path path = tempFile.getPath();

        Assertions.assertTrue(Files.exists(path), "Temp file should exist during test");

        // Write some test data
        Files.writeString(path, "test data from method 1");

        Assertions.assertEquals("test data from method 1", Files.readString(path));
    }

    @Test
    public void testMethod2(@RestResource final TempFile tempFile) throws IOException {
        Assertions.assertNotNull(tempFile, "Temp file should be injected");
        final Path path = tempFile.getPath();

        Assertions.assertTrue(Files.exists(path), "Temp file should exist during test");

        // This is a fresh temp file (not the one from testMethod1)
        Assertions.assertFalse(Files.readString(path).contains("method 1"),
                "Should be a new temp file, not reused from previous method");

        // Write different test data
        Files.writeString(path, "test data from method 2");
    }
}
