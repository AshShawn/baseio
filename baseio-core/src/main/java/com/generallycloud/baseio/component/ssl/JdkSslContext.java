/*
 * Copyright 2015-2017 GenerallyCloud.com
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.generallycloud.baseio.component.ssl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;

import com.generallycloud.baseio.common.LoggerUtil;
import com.generallycloud.baseio.log.Logger;
import com.generallycloud.baseio.log.LoggerFactory;

/**
 * An {@link SslContext} which uses JDK's SSL/TLS implementation.
 */
public class JdkSslContext extends SslContext {

    private static final Logger logger   = LoggerFactory.getLogger(JdkSslContext.class);
    static final String         PROTOCOL = "TLS";
    static final String[]       ENABLED_PROTOCOLS;
    static final Set<String>    SUPPORTED_CIPHERS;
    static final List<String>   ENABLED_CIPHERS;

    static {
        SSLContext context;
        int i;
        try {
            context = SSLContext.getInstance(PROTOCOL);
            context.init(null, null, null);
        } catch (Exception e) {
            throw new Error("failed to initialize the default SSL context", e);
        }

        SSLEngine engine = context.createSSLEngine();

        // Choose the sensible default list of protocols.
        final String[] supportedProtocols = engine.getSupportedProtocols();
        Set<String> supportedProtocolsSet = new HashSet<>(supportedProtocols.length);
        for (i = 0; i < supportedProtocols.length; ++i) {
            supportedProtocolsSet.add(supportedProtocols[i]);
        }
        List<String> protocols = new ArrayList<>();
        addIfSupported(supportedProtocolsSet, protocols, "TLSv1.2", "TLSv1.1", "TLSv1");

        if (!protocols.isEmpty()) {
            ENABLED_PROTOCOLS = protocols.toArray(new String[protocols.size()]);
        } else {
            ENABLED_PROTOCOLS = engine.getEnabledProtocols();
        }

        // Choose the sensible default list of cipher suites.
        final String[] supportedCiphers = engine.getSupportedCipherSuites();
        SUPPORTED_CIPHERS = new HashSet<>(supportedCiphers.length);
        for (i = 0; i < supportedCiphers.length; ++i) {
            SUPPORTED_CIPHERS.add(supportedCiphers[i]);
        }
        List<String> enabledCiphers = new ArrayList<>();
        addIfSupported(SUPPORTED_CIPHERS, enabledCiphers,
                // XXX: Make sure to sync this list with
                // OpenSslEngineFactory.
                // GCM (Galois/Counter Mode) requires JDK 8.
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                // AES256 requires JCE unlimited strength jurisdiction
                // policy files.
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                // GCM (Galois/Counter Mode) requires JDK 8.
                "TLS_RSA_WITH_AES_128_GCM_SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA",
                // AES256 requires JCE unlimited strength jurisdiction
                // policy files.
                "TLS_RSA_WITH_AES_256_CBC_SHA");

        if (enabledCiphers.isEmpty()) {
            // Use the default from JDK as fallback.
            for (String cipher : engine.getEnabledCipherSuites()) {
                if (cipher.contains("_RC4_")) {
                    continue;
                }
                enabledCiphers.add(cipher);
            }
        }
        ENABLED_CIPHERS = Collections.unmodifiableList(enabledCiphers);

        LoggerUtil.prettyLog(logger, "Default protocols (JDK): {} ", Arrays.asList(ENABLED_PROTOCOLS));
        LoggerUtil.prettyLog(logger, "Default cipher suites (JDK): {}", ENABLED_CIPHERS);
    }

    private static void addIfSupported(Set<String> supported, List<String> enabled,
            String... names) {
        for (String n : names) {
            if (supported.contains(n)) {
                enabled.add(n);
            }
        }
    }

    private final JdkApplicationProtocolNegotiator apn;
    private final String[]                         cipherSuites;
    private final ClientAuth                       clientAuth;
    private final boolean                         isClient;
    private final SSLContext                       sslContext;
    private final List<String>                     unmodifiableCipherSuites;

    protected JdkSslContext(SSLContext sslContext, boolean isClient, List<String> ciphers,
            JdkApplicationProtocolNegotiator apn, ClientAuth clientAuth) {
        this.apn = apn;
        this.clientAuth = clientAuth;
        this.cipherSuites = filterCipherSuites(ciphers, ENABLED_CIPHERS, SUPPORTED_CIPHERS);
        this.unmodifiableCipherSuites = Collections.unmodifiableList(Arrays.asList(cipherSuites));
        this.sslContext = sslContext;
        this.isClient = isClient;
    }

    @Override
    public final JdkApplicationProtocolNegotiator applicationProtocolNegotiator() {
        return apn;
    }

    @Override
    public final List<String> cipherSuites() {
        return unmodifiableCipherSuites;
    }

    private SSLEngine configureAndWrapEngine(SSLEngine engine) {
        engine.setEnabledCipherSuites(cipherSuites);
        engine.setEnabledProtocols(ENABLED_PROTOCOLS);
        engine.setUseClientMode(isClient());
        if (isServer()) {
            if (clientAuth == ClientAuth.OPTIONAL) {
                engine.setWantClientAuth(true);
            }else if(clientAuth == ClientAuth.REQUIRE){
                engine.setNeedClientAuth(true);
            }
        }
        return apn.wrapperFactory().wrapSslEngine(engine, apn, isServer());
    }

    /**
     * Returns the JDK {@link SSLContext} object held by this context.
     */
    private final SSLContext context() {
        return sslContext;
    }

    private String[] filterCipherSuites(List<String> ciphers, List<String> defaultCiphers,
            Set<String> supportedCiphers) {
        if (ciphers == null) {
            return defaultCiphers.toArray(new String[defaultCiphers.size()]);
        } else {
            List<String> newCiphers = new ArrayList<>();
            for (String c : ciphers) {
                if (c == null) {
                    break;
                }
                if (supportedCiphers.contains(c)) {
                    newCiphers.add(c);
                }
            }
            return newCiphers.toArray(new String[newCiphers.size()]);
        }
    }

    @Override
    public final boolean isClient() {
        return isClient;
    }

    @Override
    public final SSLEngine newEngine() {
        return configureAndWrapEngine(context().createSSLEngine());
    }

    @Override
    public final SSLEngine newEngine(String peerHost, int peerPort) {
        return configureAndWrapEngine(context().createSSLEngine(peerHost, peerPort));
    }

    @Override
    public final long sessionCacheSize() {
        return sessionContext().getSessionCacheSize();
    }

    @Override
    public final SSLSessionContext sessionContext() {
        if (isServer()) {
            return context().getServerSessionContext();
        } else {
            return context().getClientSessionContext();
        }
    }

    @Override
    public final long sessionTimeout() {
        return sessionContext().getSessionTimeout();
    }

}
