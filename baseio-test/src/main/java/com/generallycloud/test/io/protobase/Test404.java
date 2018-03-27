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

import com.generallycloud.baseio.codec.protobase.ParamedProtobaseProtocolFactory;
import com.generallycloud.baseio.codec.protobase.future.ProtobaseFuture;
import com.generallycloud.baseio.codec.protobase.future.ProtobaseFutureImpl;
import com.generallycloud.baseio.common.CloseUtil;
import com.generallycloud.baseio.component.LoggerSocketSEListener;
import com.generallycloud.baseio.component.NioSocketChannelContext;
import com.generallycloud.baseio.component.SocketChannelContext;
import com.generallycloud.baseio.configuration.ServerConfiguration;
import com.generallycloud.baseio.connector.SocketChannelConnector;
import com.generallycloud.baseio.container.FixedSession;
import com.generallycloud.baseio.container.SimpleIoEventHandle;
import com.generallycloud.baseio.log.LoggerFactory;
import com.generallycloud.baseio.protocol.Future;

public class Test404 {

    public static void main(String[] args) throws Exception {

        String serviceKey = "22";

        LoggerFactory.configure();

        SimpleIoEventHandle eventHandle = new SimpleIoEventHandle();

        ServerConfiguration configuration = new ServerConfiguration(8300);

        SocketChannelContext context = new NioSocketChannelContext(configuration);

        SocketChannelConnector connector = new SocketChannelConnector(context);

        context.setIoEventHandleAdaptor(eventHandle);

        context.setProtocolFactory(new ParamedProtobaseProtocolFactory());

        context.addSessionEventListener(new LoggerSocketSEListener());

        FixedSession session = new FixedSession(connector.connect());

        ProtobaseFuture future = session.request(serviceKey, null);

        System.out.println(future.getReadText());

        Future future1 = new ProtobaseFutureImpl(connector.getContext()).setPING();

        session.getSession().flush(future1);

        CloseUtil.close(connector);
    }
}
