/**
 * Copyright (c) 2025-2026, Michael Yang 杨福海 (fuhai999@gmail.com).
 * <p>
 * Licensed under the GNU Lesser General Public License (LGPL) ,Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.tinyflow.core.util;

import okhttp3.OkHttpClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for creating and configuring OkHttpClient instances.
 * <p>
 * By default, it uses secure TLS settings. Insecure HTTPS (trust-all) can be enabled
 * via system property {@code tinyflow.okhttp.insecure=true}, but it is strongly
 * discouraged in production environments.
 * </p>
 */
public final class OkHttpClientUtil {

    private static final Logger LOGGER = Logger.getLogger(OkHttpClientUtil.class.getName());

    private static volatile OkHttpClient.Builder customBuilder;
    private static final Object LOCK = new Object();

    // Prevent instantiation
    private OkHttpClientUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Sets a custom OkHttpClient.Builder to be used by {@link #buildDefaultClient()}.
     * This should be called during application initialization.
     */
    public static void setCustomBuilder(OkHttpClient.Builder builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Builder must not be null");
        }
        customBuilder = builder;
    }

    /**
     * Returns a shared default OkHttpClient instance with reasonable timeouts and optional proxy.
     * If a custom builder was set via {@link #setCustomBuilder}, it will be used.
     * <p>
     * SSL is secure by default. Insecure mode (trust-all) can be enabled via system property:
     * {@code -Dtinyflow.okhttp.insecure=true}
     * </p>
     */
    public static OkHttpClient buildDefaultClient() {
        OkHttpClient.Builder builder = customBuilder;
        if (builder != null) {
            return builder.build();
        }

        synchronized (LOCK) {
            // Double-check in case another thread set it while waiting
            builder = customBuilder;
            if (builder != null) {
                return builder.build();
            }

            builder = new OkHttpClient.Builder()
                    .connectTimeout(1, TimeUnit.MINUTES)
                    .readTimeout(5, TimeUnit.MINUTES);

            // Optional insecure mode (for development/testing only)
            if (isInsecureModeEnabled()) {
                LOGGER.warning("OkHttpClient is running in INSECURE mode (trust-all SSL). " +
                        "This is dangerous and should not be used in production.");
                enableInsecureSsl(builder);
            }

            configureProxy(builder);
            return builder.build();
        }
    }

    private static boolean isInsecureModeEnabled() {
        return Boolean.parseBoolean(System.getProperty("tinyflow.okhttp.insecure", "false"));
    }


    private static void enableInsecureSsl(OkHttpClient.Builder builder) {
        try {
            X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            builder.sslSocketFactory(sslSocketFactory, trustManager)
                    .hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure insecure SSL for OkHttpClient", e);
        }
    }

    private static void configureProxy(OkHttpClient.Builder builder) {
        String proxyHost = getProxyHost();
        String proxyPort = getProxyPort();

        if (StringUtil.hasText(proxyHost) && StringUtil.hasText(proxyPort)) {
            try {
                int port = Integer.parseInt(proxyPort.trim());
                InetSocketAddress addr = new InetSocketAddress(proxyHost.trim(), port);
                builder.proxy(new Proxy(Proxy.Type.HTTP, addr));
                LOGGER.fine("Configured HTTP proxy: " + proxyHost + ":" + port);
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Invalid proxy port: " + proxyPort, e);
            }
        }
    }

    private static String getProxyHost() {
        String host = System.getProperty("https.proxyHost");
        if (!StringUtil.hasText(host)) {
            host = System.getProperty("http.proxyHost");
        }
        return host;
    }

    private static String getProxyPort() {
        String port = System.getProperty("https.proxyPort");
        if (!StringUtil.hasText(port)) {
            port = System.getProperty("http.proxyPort");
        }
        return port;
    }
}