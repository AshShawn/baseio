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
package com.generallycloud.test.io.protobase;

import com.generallycloud.baseio.codec.protobase.ProtobaseProtocolFactory;
import com.generallycloud.baseio.codec.protobase.future.ProtobaseFuture;
import com.generallycloud.baseio.common.CloseUtil;
import com.generallycloud.baseio.component.LoggerSocketSEListener;
import com.generallycloud.baseio.component.NioSocketChannelContext;
import com.generallycloud.baseio.component.SocketChannelContext;
import com.generallycloud.baseio.configuration.ServerConfiguration;
import com.generallycloud.baseio.connector.SocketChannelConnector;
import com.generallycloud.baseio.container.FixedSession;
import com.generallycloud.baseio.container.SimpleIoEventHandle;
import com.generallycloud.baseio.log.LoggerFactory;

public class TestStopServer {

    public static void main(String[] args) throws Exception {

        LoggerFactory.configure();

        SimpleIoEventHandle eventHandle = new SimpleIoEventHandle();

        ServerConfiguration configuration = new ServerConfiguration(8300);

        SocketChannelContext context = new NioSocketChannelContext(configuration);

        SocketChannelConnector connector = new SocketChannelConnector(context);

        context.setIoEventHandleAdaptor(eventHandle);

        context.setProtocolFactory(new ProtobaseProtocolFactory());

        context.addSessionEventListener(new LoggerSocketSEListener());

        FixedSession session = new FixedSession(connector.connect());

        ProtobaseFuture future = session.request("test-stop-server2.auth", null);

        System.out.println(future.getReadText());

        CloseUtil.close(connector);

    }
}
