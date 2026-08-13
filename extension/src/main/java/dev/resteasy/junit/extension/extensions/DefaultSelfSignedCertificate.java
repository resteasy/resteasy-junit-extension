/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.junit.extension.extensions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.JUnitException;

import dev.resteasy.junit.extension.api.SelfSignedCertificate;

/**
 * Default implementation of {@link SelfSignedCertificate} that generates self-signed certificates using the JDK
 * {@code keytool} command.
 * <p>
 * Instances are created via the {@link #getOrCreate(ExtensionContext)} factory method, which generates server and
 * client key pairs, cross-signs them into truststores, and builds {@link SSLContext} instances for both sides. All
 * generated files are stored in a temporary directory and cleaned up when {@link #close()} is called.
 * </p>
 * <p>
 * This class is not intended for direct use by test authors. It is managed by the extension framework and
 * accessible through the {@link SelfSignedCertificate} interface via
 * {@link dev.resteasy.junit.extension.annotations.SslCert @SslCert} injection.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class DefaultSelfSignedCertificate implements SelfSignedCertificate, AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(DefaultSelfSignedCertificate.class);
    private static final String KEY = "SelfSignedCertificate";

    private static final String ALIAS = "self-signed";
    private static final String CLIENT_DN = "CN=localhost, OU=Test, L=Test, ST=Test, C=Test";
    private static final String SERVER_DN = "CN=localhost, OU=Unknown, L=Unknown, ST=Unknown, C=Unknown";
    private static final String KEYSTORE_TYPE = "PKCS12";

    private final Path tempDir;
    private final SslContextHolder serverHolder;
    private final SslContextHolder clientHolder;

    private DefaultSelfSignedCertificate(final Path tempDir, final SslContextHolder serverHolder,
            final SslContextHolder clientHolder) {
        this.tempDir = tempDir;
        this.serverHolder = serverHolder;
        this.clientHolder = clientHolder;
    }

    /**
     * Gets the current {@link DefaultSelfSignedCertificate}.
     *
     * @return a certificate holder
     *
     */
    static DefaultSelfSignedCertificate get(final ExtensionContext context) {
        return getStore(context).get(KEY, DefaultSelfSignedCertificate.class);
    }

    /**
     * Creates or gets a {@link DefaultSelfSignedCertificate from the context provided. If creating a new cert is
     * required, the {@code keytool} command is used to generate a self-signed certificate for both the server and the
     * client.
     * <p>
     * On failure, any partially created files and the temporary directory are cleaned up before the exception is
     * propagated.
     * </p>
     *
     * @return a fully initialized certificate holder
     *
     */
    static DefaultSelfSignedCertificate getOrCreate(final ExtensionContext context) {
        final ExtensionContext.Store store = getStore(context);
        return store.getOrComputeIfAbsent(KEY, CertFunction.INSTANCE, DefaultSelfSignedCertificate.class);
    }

    @Override
    public SSLContext clientSslContext() {
        return clientHolder.sslContext;
    }

    @Override
    public KeyStore clientKeyStore() {
        return clientHolder.keyStore;
    }

    @Override
    public Path clientKeyStorePath() {
        return clientHolder.keyStoreFile;
    }

    @Override
    public KeyStore clientTrustStore() {
        return clientHolder.trustStore;
    }

    @Override
    public Path clientTrustStorePath() {
        return clientHolder.trustStoreFile;
    }

    @Override
    public SSLContext serverSslContext() {
        return serverHolder.sslContext;
    }

    @Override
    public KeyStore serverKeyStore() {
        return serverHolder.keyStore;
    }

    @Override
    public Path serverKeyStorePath() {
        return serverHolder.keyStoreFile;
    }

    @Override
    public KeyStore serverTrustStore() {
        return serverHolder.trustStore;
    }

    @Override
    public Path serverTrustStorePath() {
        return serverHolder.trustStoreFile;
    }

    @Override
    public String keyStorePassword() {
        return KEYSTORE_PASSWORD;
    }

    @Override
    public String keyStoreType() {
        return KEYSTORE_TYPE;
    }

    @Override
    public void close() {
        deleteRecursively(tempDir);
    }

    private static SslContextHolder createSslContext(final Path keyStoreFile, final Path trustStoreFile) throws Exception {
        final KeyStore keyStore = loadKeyStore(keyStoreFile);
        final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

        final KeyStore trustStore = loadKeyStore(trustStoreFile);
        final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return new SslContextHolder(sslContext, keyStore, keyStoreFile, trustStore, trustStoreFile);
    }

    private static KeyStore loadKeyStore(final Path file) throws Exception {
        final KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);
        try (InputStream in = Files.newInputStream(file)) {
            ks.load(in, KEYSTORE_PASSWORD.toCharArray());
        }
        return ks;
    }

    private static void keytool(final ForkJoinPool executor, final String... args) throws Exception {
        final String keytoolCmd = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
        final List<String> command = new ArrayList<>(args.length + 1);
        command.add(keytoolCmd);
        command.addAll(List.of(args));
        final Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            final Runnable consoleConsumer = () -> {
                final byte[] buffer = new byte[256];
                int len;
                try (InputStream in = process.getInputStream()) {
                    while ((len = in.read(buffer)) != -1) {
                        output.write(buffer, 0, len);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
            final ForkJoinTask<?> task = executor.submit(consoleConsumer);
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                task.cancel(true);
                process.destroyForcibly();
                throw new JUnitException(String.format("keytool did not exit within 30 seconds: %s", output));
            }
            final int exitCode = process.exitValue();
            if (exitCode != 0) {
                task.join();
                throw new IOException(String.format("keytool failed with exit code %d: %s", exitCode, output));
            }
        }
    }

    private static void deleteRecursively(final Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                    safeDelete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) {
                    safeDelete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.debugf(e, "Failed to recursively delete directory %s", dir);
        }
    }

    private static void safeDelete(final Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LOGGER.debugf(e, "Failed to delete file %s", path);
        }
    }

    private static ExtensionContext.Store getStore(final ExtensionContext context) {
        return context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
    }

    private static class SslContextHolder {
        final SSLContext sslContext;
        final KeyStore keyStore;
        final Path keyStoreFile;
        final KeyStore trustStore;
        final Path trustStoreFile;

        SslContextHolder(final SSLContext sslContext, final KeyStore keyStore, final Path keyStoreFile,
                final KeyStore trustStore, final Path trustStoreFile) {
            this.sslContext = sslContext;
            this.keyStore = keyStore;
            this.keyStoreFile = keyStoreFile;
            this.trustStore = trustStore;
            this.trustStoreFile = trustStoreFile;
        }
    }

    private static class CertFunction implements Function<String, DefaultSelfSignedCertificate> {
        static final CertFunction INSTANCE = new CertFunction();

        @Override
        public DefaultSelfSignedCertificate apply(final String s) {
            @SuppressWarnings("resource")
            final ForkJoinPool executor = ForkJoinPool.commonPool();
            try {
                final Path tempDir = Files.createTempDirectory("resteasy-ssl-");

                final Path serverKeyStoreFile = tempDir.resolve("server.keystore");
                final Path serverTrustStoreFile = tempDir.resolve("server.truststore");
                final Path clientKeyStoreFile = tempDir.resolve("client.keystore");
                final Path clientTrustStoreFile = tempDir.resolve("client.truststore");

                final Path serverCertFile = tempDir.resolve("server.cer");
                final Path clientCertFile = tempDir.resolve("client.cer");

                try {
                    // Generate server key pair and export its certificate
                    keytool(executor, "-genkeypair", "-alias", ALIAS,
                            "-keyalg", "RSA", "-keysize", "2048",
                            "-storetype", KEYSTORE_TYPE,
                            "-keystore", serverKeyStoreFile.toString(),
                            "-storepass", KEYSTORE_PASSWORD,
                            "-dname", SERVER_DN);
                    keytool(executor, "-exportcert", "-alias", ALIAS,
                            "-keystore", serverKeyStoreFile.toString(),
                            "-storepass", KEYSTORE_PASSWORD,
                            "-file", serverCertFile.toString());

                    // Generate client key pair and export its certificate
                    keytool(executor, "-genkeypair", "-alias", ALIAS,
                            "-keyalg", "RSA", "-keysize", "2048",
                            "-storetype", KEYSTORE_TYPE,
                            "-keystore", clientKeyStoreFile.toString(),
                            "-storepass", KEYSTORE_PASSWORD,
                            "-dname", CLIENT_DN);
                    keytool(executor, "-exportcert", "-alias", ALIAS,
                            "-keystore", clientKeyStoreFile.toString(),
                            "-storepass", KEYSTORE_PASSWORD,
                            "-file", clientCertFile.toString());

                    // Import server cert into client truststore (so client trusts server)
                    keytool(executor, "-importcert", "-alias", "server",
                            "-storetype", KEYSTORE_TYPE,
                            "-keystore", clientTrustStoreFile.toString(),
                            "-storepass", KEYSTORE_PASSWORD,
                            "-file", serverCertFile.toString(),
                            "-noprompt");

                    // Import client cert into server truststore (so server trusts client)
                    keytool(executor, "-importcert", "-alias", "client",
                            "-storetype", KEYSTORE_TYPE,
                            "-keystore", serverTrustStoreFile.toString(),
                            "-storepass", KEYSTORE_PASSWORD,
                            "-file", clientCertFile.toString(),
                            "-noprompt");
                    final SslContextHolder server = createSslContext(serverKeyStoreFile, serverTrustStoreFile);
                    final SslContextHolder client = createSslContext(clientKeyStoreFile, clientTrustStoreFile);
                    return new DefaultSelfSignedCertificate(tempDir, server, client);
                } catch (Exception e) {
                    deleteRecursively(tempDir);
                    if (e instanceof IOException) {
                        throw (IOException) e;
                    }
                    if (e instanceof JUnitException) {
                        throw (JUnitException) e;
                    }
                    throw new JUnitException("Failed to generate SSL contexts", e);
                } finally {
                    try {
                        Files.deleteIfExists(serverCertFile);
                    } catch (Exception e) {
                        LOGGER.warnf(e, "Could not delete server certificate file %s", serverCertFile);
                    }
                    try {
                        Files.deleteIfExists(clientCertFile);
                    } catch (Exception e) {
                        LOGGER.warnf(e, "Could not delete client certificate file %s", clientCertFile);
                    }
                }
            } catch (IOException e) {
                throw new JUnitException("Failed to generate SSL contexts", e);
            }
        }
    }
}
