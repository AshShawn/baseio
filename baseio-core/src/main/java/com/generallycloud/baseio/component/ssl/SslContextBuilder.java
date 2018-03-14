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

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.generallycloud.baseio.common.CloseUtil;
import com.generallycloud.baseio.component.ByteArrayInputStream;
import com.generallycloud.baseio.component.ssl.ApplicationProtocolConfig.Protocol;
import com.generallycloud.baseio.component.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import com.generallycloud.baseio.component.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;

/**
 * Builder for configuring a new SslContext for creation.
 */
public final class SslContextBuilder {

    public SslContextBuilder(boolean isServer) {
        this.isServer = isServer;
    }

    public static SslContextBuilder forClient(boolean trustAll) {
        return new SslContextBuilder(false).trustManager(trustAll);
    }

    public static SslContextBuilder forServer(InputStream certChainInputStream,
            InputStream keyInputStream, String keyPassword) {
        return new SslContextBuilder(true).keyManager(certChainInputStream, keyInputStream,
                keyPassword);
    }

    public static SslContextBuilder forServer(PrivateKey key, String keyPassword,
            X509Certificate... keyCertChain) {
        return new SslContextBuilder(true).keyManager(key, keyPassword, keyCertChain);
    }

    public static SslContextBuilder forServer(KeyManagerFactory keyManagerFactory) {
        return new SslContextBuilder(true).keyManager(keyManagerFactory);
    }

    public static SslContextBuilder forServer(InputStream ks, String storePassword, String alias,
            String keyPassword) throws SSLException {
        return new SslContextBuilder(true).keyManager(ks, storePassword, alias, keyPassword);
    }

    private PKCS8EncodedKeySpec generateKeySpec(String password, byte[] key)
            throws IOException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException {
        if (password == null) {
            return new PKCS8EncodedKeySpec(key);
        }
        EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(key);
        SecretKeyFactory keyFactory = SecretKeyFactory
                .getInstance(encryptedPrivateKeyInfo.getAlgName());
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());
        SecretKey pbeKey = keyFactory.generateSecret(pbeKeySpec);
        Cipher cipher = Cipher.getInstance(encryptedPrivateKeyInfo.getAlgName());
        cipher.init(Cipher.DECRYPT_MODE, pbeKey, encryptedPrivateKeyInfo.getAlgParameters());
        return encryptedPrivateKeyInfo.getKeySpec(cipher);
    }

    private X509Certificate[] getCertificatesFromBuffers(List<byte[]> certs)
            throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate[] x509Certs = new X509Certificate[certs.size()];
        for (int i = 0; i < certs.size(); i++) {
            x509Certs[i] = (X509Certificate) cf
                    .generateCertificate(new ByteArrayInputStream(certs.get(i)));
        }
        return x509Certs;
    }

    private PrivateKey getPrivateKeyFromByteBuffer(byte[] encodedKey, String keyPassword)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
            InvalidAlgorithmParameterException, KeyException, IOException {

        PKCS8EncodedKeySpec encodedKeySpec = generateKeySpec(keyPassword, encodedKey);
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(encodedKeySpec);
        } catch (InvalidKeySpecException ignore) {}
        try {
            return KeyFactory.getInstance("DSA").generatePrivate(encodedKeySpec);
        } catch (InvalidKeySpecException ignore2) {}
        try {
            return KeyFactory.getInstance("EC").generatePrivate(encodedKeySpec);
        } catch (InvalidKeySpecException e) {
            throw new InvalidKeySpecException("Neither RSA, DSA nor EC worked", e);
        }
    }

    private JdkApplicationProtocolNegotiator toNegotiator(ApplicationProtocolConfig config,
            boolean isServer) {
        if (config == null) {
            return new JdkDefaultApplicationProtocolNegotiator();
        }
        Protocol p = config.protocol();
        if (p == Protocol.NONE) {
            return new JdkDefaultApplicationProtocolNegotiator();
        } else if (p == Protocol.ALPN) {
            if (isServer) {
                SelectorFailureBehavior sfb = config.selectorFailureBehavior();
                if (sfb == SelectorFailureBehavior.FATAL_ALERT) {
                    return new JdkAlpnApplicationProtocolNegotiator(true,
                            config.supportedProtocols());
                } else if (sfb == SelectorFailureBehavior.NO_ADVERTISE) {
                    new JdkAlpnApplicationProtocolNegotiator(false, config.supportedProtocols());
                }
                throw new UnsupportedOperationException("JDK provider does not support "
                        + config.selectorFailureBehavior() + " failure behavior");
            } else {
                SelectedListenerFailureBehavior slfb = config.selectedListenerFailureBehavior();
                if (slfb == SelectedListenerFailureBehavior.ACCEPT) {
                    return new JdkAlpnApplicationProtocolNegotiator(false,
                            config.supportedProtocols());
                } else if (slfb == SelectedListenerFailureBehavior.FATAL_ALERT) {
                    return new JdkAlpnApplicationProtocolNegotiator(true,
                            config.supportedProtocols());
                }
                throw new UnsupportedOperationException("JDK provider does not support "
                        + config.selectedListenerFailureBehavior() + " failure behavior");
            }
        }
        throw new UnsupportedOperationException(
                "JDK provider does not support " + config.protocol() + " protocol");
    }

    private SSLContext newSSLContext() throws SSLException {
        if (isServer && keyManagerFactory == null) {
            if (key == null) {
                throw new SSLException("null of key or keyManagerFactory");
            }
            keyManagerFactory = buildKeyManagerFactory(keyCertChain, key, keyPassword);
        }
        if (trustCertCollection != null) {
            trustManagerFactory = buildTrustManagerFactory(trustCertCollection);
        }
        return newSSLContext(trustManagerFactory, keyManagerFactory, x509TrustManager, isServer,
                trustAll, sessionCacheSize, sessionTimeout);
    }

    private SSLContext newSSLContext(TrustManagerFactory trustManagerFactory,
            KeyManagerFactory keyManagerFactory, X509TrustManager x509TrustManager,
            boolean isServer, boolean trustAll, long sessionCacheSize, long sessionTimeout)
            throws SSLException {
        if (isServer && keyManagerFactory == null) {
            throw new SSLException("null keyManagerFactory on server");
        }
        try {
            SSLContext ctx = SslContext.getSSLContext();
            TrustManager[] tms = null;
            KeyManager[] kms = null;
            if (keyManagerFactory == null) {
                // client
                if (trustManagerFactory == null) {
                    if (x509TrustManager != null) {
                        tms = new X509TrustManager[] { x509TrustManager };
                    } else {
                        if (trustAll) {
                            X509TrustManager x509m = new X509TrustManager() {

                                @Override
                                public void checkClientTrusted(
                                        java.security.cert.X509Certificate[] arg0, String arg1)
                                        throws java.security.cert.CertificateException {}

                                @Override
                                public void checkServerTrusted(
                                        java.security.cert.X509Certificate[] arg0, String arg1)
                                        throws java.security.cert.CertificateException {}

                                @Override
                                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                    return null;
                                }
                            };
                            tms = new X509TrustManager[] { x509m };
                        }
                    }
                } else {
                    tms = trustManagerFactory.getTrustManagers();
                }
            } else {
                kms = keyManagerFactory.getKeyManagers();
                if (trustManagerFactory != null) {
                    tms = trustManagerFactory.getTrustManagers();
                }
            }
            ctx.init(kms, tms, new SecureRandom());
            SSLSessionContext sessCtx = ctx.getClientSessionContext();
            if (sessionCacheSize > 0) {
                sessCtx.setSessionCacheSize((int) Math.min(sessionCacheSize, Integer.MAX_VALUE));
            }
            if (sessionTimeout > 0) {
                sessCtx.setSessionTimeout((int) Math.min(sessionTimeout, Integer.MAX_VALUE));
            }
            return ctx;
        } catch (Exception e) {
            if (e instanceof SSLException) {
                throw (SSLException) e;
            }
            throw new SSLException("failed to initialize the SSL context", e);
        }
    }

    private TrustManagerFactory buildTrustManagerFactory(X509Certificate[] certCollection)
            throws SSLException {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(null, null);
            int i = 1;
            for (X509Certificate cert : certCollection) {
                String alias = Integer.toString(i);
                ks.setCertificateEntry(alias, cert);
                i++;
            }
            // Set up trust manager factory to use our key store.
            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(ks);
            return trustManagerFactory;
        } catch (Exception e) {
            throw new SSLException(e);
        }
    }

    private KeyManagerFactory buildKeyManagerFactory(X509Certificate[] certChain, PrivateKey key,
            String keyPassword) throws SSLException {
        if (keyPassword == null) {
            keyPassword = "";
        }
        char[] keyPasswordChars = keyPassword.toCharArray();
        KeyStore ks = buildKeyStore(certChain, key, keyPasswordChars);
        return buildKeyManagerFactory(ks, keyPasswordChars);
    }

    private KeyManagerFactory buildKeyManagerFactory(KeyStore ks, char[] keyPasswordChars)
            throws SSLException {
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }
        // Set up key manager factory to use our key store
        try {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, keyPasswordChars);
            return kmf;
        } catch (Exception e) {
            throw new SSLException(e);
        }
    }

    private KeyStore buildKeyStore(X509Certificate[] certChain, PrivateKey key,
            char[] keyPasswordChars) throws SSLException {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(null, null);
            ks.setKeyEntry("key", key, keyPasswordChars, certChain);
            return ks;
        } catch (Exception e) {
            throw new SSLException(e);
        }
    }

    private PrivateKey toPrivateKey(InputStream keyInputStream, String keyPassword)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
            InvalidAlgorithmParameterException, KeyException, IOException, CertificateException {
        if (keyInputStream == null) {
            return null;
        }
        return getPrivateKeyFromByteBuffer(PemReader.readCertificates(keyInputStream).get(0), keyPassword);
    }

    private X509Certificate[] toX509Certificates(InputStream in) throws CertificateException {
        if (in == null) {
            throw new IllegalArgumentException("null inputstream");
        }
        return getCertificatesFromBuffers(PemReader.readCertificates(in));
    }

    private ApplicationProtocolConfig apn;

    private long                      sessionCacheSize;
    private long                      sessionTimeout;
    private boolean                   isServer;

    private void needServer() {
        if (!isServer) {
            throw new IllegalArgumentException("server context mode");
        }
    }

    private void needClient() {
        if (isServer) {
            throw new IllegalArgumentException("client context mode");
        }
    }

    public SslContextBuilder applicationProtocolConfig(ApplicationProtocolConfig apn) {
        this.apn = apn;
        return this;
    }

    public SslContextBuilder sessionCacheSize(long sessionCacheSize) {
        this.sessionCacheSize = sessionCacheSize;
        return this;
    }

    public SslContextBuilder sessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
        return this;
    }

    /*----------------------------------------- server begin --------------------------------------*/

    private List<String>      ciphers;
    private ClientAuth        clientAuth = ClientAuth.NONE;
    private PrivateKey        key;
    private X509Certificate[] keyCertChain;
    private KeyManagerFactory keyManagerFactory;
    private String            keyPassword;

    public SslContextBuilder ciphers(List<String> ciphers) {
        needServer();
        this.ciphers = ciphers;
        return this;
    }

    public SslContextBuilder clientAuth(ClientAuth clientAuth) {
        needServer();
        this.clientAuth = clientAuth;
        return this;
    }

    public SslContextBuilder keyManager(KeyManagerFactory keyManagerFactory) {
        needServer();
        this.keyCertChain = null;
        this.key = null;
        this.keyPassword = null;
        this.keyManagerFactory = keyManagerFactory;
        return this;
    }

    public SslContextBuilder keyManager(InputStream keystoreInput, String storePassword,
            String alias, String keyPassword) throws SSLException {
        needServer();
        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(keystoreInput, storePassword.toCharArray());
            if (keyPassword == null) {
                keyPassword = "";
            }
            this.keyManagerFactory = buildKeyManagerFactory(keystore, keyPassword.toCharArray());
            return this;
        } catch (Exception e) {
            throw new SSLException(e);
        }
    }

    public SslContextBuilder keyManager(PrivateKey key, String keyPassword,
            X509Certificate... keyCertChain) {
        needServer();
        if (keyCertChain.length == 0) {
            throw new IllegalArgumentException("keyCertChain must be non-empty");
        }
        if (keyCertChain == null || keyCertChain.length == 0) {
            this.keyCertChain = null;
        } else {
            for (X509Certificate cert : keyCertChain) {
                if (cert == null) {
                    throw new IllegalArgumentException("keyCertChain contains null entry");
                }
            }
            this.keyCertChain = keyCertChain.clone();
        }
        this.key = key;
        this.keyPassword = keyPassword;
        this.keyManagerFactory = null;
        return this;
    }

    public SslContextBuilder keyManager(InputStream certChainInputStream,
            InputStream keyInputStream, String keyPassword) {
        needServer();
        X509Certificate[] keyCertChain;
        PrivateKey key;
        try {
            keyCertChain = toX509Certificates(certChainInputStream);
        } catch (Exception e) {
            throw new IllegalArgumentException("Input stream not contain valid certificates.", e);
        } finally {
            CloseUtil.close(certChainInputStream);
        }
        try {
            key = toPrivateKey(keyInputStream, keyPassword);
        } catch (Exception e) {
            throw new IllegalArgumentException("Input stream does not contain valid private key.",
                    e);
        } finally {
            CloseUtil.close(keyInputStream);
        }
        return keyManager(key, keyPassword, keyCertChain);
    }

    public SslContext build() throws SSLException {
        SSLContext context = newSSLContext();
        JdkApplicationProtocolNegotiator negotiator = toNegotiator(apn, isServer);
        return new SslContext(context, !isServer, ciphers, negotiator, clientAuth);
    }

    /*----------------------------------------- server end --------------------------------------*/

    /*----------------------------------------- client begin --------------------------------------*/

    private X509Certificate[]   trustCertCollection;
    private TrustManagerFactory trustManagerFactory;
    private boolean             trustAll;
    private X509TrustManager    x509TrustManager;

    public SslContextBuilder trustManager(InputStream trustCertCollectionInputStream) {
        needClient();
        try {
            return trustManager(toX509Certificates(trustCertCollectionInputStream));
        } catch (Exception e) {
            throw new IllegalArgumentException("Input stream does not contain valid certificates.",
                    e);
        }
    }

    public SslContextBuilder trustManager(TrustManagerFactory trustManagerFactory) {
        needClient();
        this.trustCertCollection = null;
        this.trustManagerFactory = trustManagerFactory;
        return this;
    }

    public SslContextBuilder trustManager(boolean trustAll) {
        needClient();
        this.trustAll = trustAll;
        this.x509TrustManager = null;
        return this;
    }

    public SslContextBuilder trustManager(X509TrustManager x509TrustManager) {
        needClient();
        this.x509TrustManager = x509TrustManager;
        this.trustAll = false;
        return this;
    }

    public SslContextBuilder trustManager(X509Certificate... trustCertCollection) {
        needClient();
        if (trustCertCollection != null) {
            this.trustCertCollection = trustCertCollection.clone();
        }
        this.trustManagerFactory = null;
        return this;
    }

    /*----------------------------------------- client end --------------------------------------*/

}
