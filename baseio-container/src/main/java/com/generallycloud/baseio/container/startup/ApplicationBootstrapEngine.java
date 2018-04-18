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
package com.generallycloud.baseio.container.startup;

import java.io.File;

import com.generallycloud.baseio.acceptor.SocketChannelAcceptor;
import com.generallycloud.baseio.common.CloseUtil;
import com.generallycloud.baseio.common.FileUtil;
import com.generallycloud.baseio.common.Properties;
import com.generallycloud.baseio.common.StringUtil;
import com.generallycloud.baseio.component.BootstrapEngine;
import com.generallycloud.baseio.component.NioSocketChannelContext;
import com.generallycloud.baseio.component.SocketChannelContext;
import com.generallycloud.baseio.component.ssl.SSLUtil;
import com.generallycloud.baseio.component.ssl.SslContext;
import com.generallycloud.baseio.configuration.PropertiesSCLoader;
import com.generallycloud.baseio.configuration.ServerConfiguration;
import com.generallycloud.baseio.log.Logger;
import com.generallycloud.baseio.log.LoggerFactory;

public abstract class ApplicationBootstrapEngine implements BootstrapEngine {

    protected abstract void enrichSocketChannelContext(String rootPath, boolean deployModel,
            SocketChannelContext context);

    @Override
    public void bootstrap(String rootPath, boolean deployModel) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        Properties serverProperties = FileUtil.readPropertiesByCls("server.properties");
        ServerConfiguration sc = new ServerConfiguration();
        new PropertiesSCLoader("SERVER").loadConfiguration(sc, serverProperties);
        SocketChannelContext context = new NioSocketChannelContext(sc);
        SocketChannelAcceptor acceptor = new SocketChannelAcceptor(context);
        enrichSocketChannelContext(rootPath, deployModel, context);
        try {
            if (sc.isSERVER_ENABLE_SSL()) {
                if (!StringUtil.isNullOrBlank(sc.getSERVER_CERT_KEY())) {
                    File certificate = FileUtil.readFileByCls(sc.getSERVER_CERT_CRT(), classLoader);
                    File privateKey = FileUtil.readFileByCls(sc.getSERVER_CERT_KEY(), classLoader);
                    SslContext sslContext = SSLUtil.initServer(privateKey, certificate);
                    context.setSslContext(sslContext);
                } else {
                    String keystoreInfo = sc.getSERVER_SSL_KEYSTORE();
                    if (StringUtil.isNullOrBlank(keystoreInfo)) {
                        throw new IllegalArgumentException("ssl enabled,but no config for");
                    }
                    String[] params = keystoreInfo.split(";");
                    if (params.length != 4) {
                        throw new IllegalArgumentException("SERVER_SSL_KEYSTORE config error");
                    }
                    File storeFile = FileUtil.readFileByCls(params[0], classLoader);
                    SslContext sslContext = SSLUtil.initServer(storeFile, params[1], params[2],
                            params[3]);
                    context.setSslContext(sslContext);
                }
            }
            int port = sc.getSERVER_PORT();
            if (port == 0) {
                port = sc.isSERVER_ENABLE_SSL() ? 443 : 80;
            }
            sc.setSERVER_PORT(port);
            acceptor.bind();
        } catch (Throwable e) {
            Logger logger = LoggerFactory.getLogger(getClass());
            logger.error(e.getMessage(), e);
            CloseUtil.unbind(acceptor);
        }
    }

}
