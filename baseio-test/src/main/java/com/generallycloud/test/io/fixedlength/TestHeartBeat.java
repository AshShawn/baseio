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
package com.generallycloud.test.io.fixedlength;

import com.generallycloud.baseio.codec.fixedlength.FixedLengthProtocolFactory;
import com.generallycloud.baseio.codec.fixedlength.future.FLBeatFutureFactory;
import com.generallycloud.baseio.codec.fixedlength.future.FixedLengthFutureImpl;
import com.generallycloud.baseio.common.CloseUtil;
import com.generallycloud.baseio.common.ThreadUtil;
import com.generallycloud.baseio.component.IoEventHandleAdaptor;
import com.generallycloud.baseio.component.LoggerSocketSEListener;
import com.generallycloud.baseio.component.NioSocketChannelContext;
import com.generallycloud.baseio.component.SocketChannelContext;
import com.generallycloud.baseio.component.SocketSession;
import com.generallycloud.baseio.component.SocketSessionActiveSEListener;
import com.generallycloud.baseio.configuration.ServerConfiguration;
import com.generallycloud.baseio.connector.SocketChannelConnector;
import com.generallycloud.baseio.log.DebugUtil;
import com.generallycloud.baseio.protocol.Future;

public class TestHeartBeat {

    public static void main(String[] args) throws Exception {

        DebugUtil.setEnableDebug(true);

        IoEventHandleAdaptor eventHandleAdaptor = new IoEventHandleAdaptor() {

            @Override
            public void accept(SocketSession session, Future future) throws Exception {
                DebugUtil.debug("______________" + future.getReadText());
            }
        };

        ServerConfiguration configuration = new ServerConfiguration(8300);

        configuration.setSERVER_SESSION_IDLE_TIME(20);

        SocketChannelContext context = new NioSocketChannelContext(configuration);

        SocketChannelConnector connector = new SocketChannelConnector(context);

        context.addSessionIdleEventListener(new SocketSessionActiveSEListener());
        
        context.addSessionEventListener(new LoggerSocketSEListener());

        context.setBeatFutureFactory(new FLBeatFutureFactory());

        context.setProtocolFactory(new FixedLengthProtocolFactory());

        context.setIoEventHandleAdaptor(eventHandleAdaptor);

        SocketSession session = connector.connect();

        String param = "tttt";

        long old = System.currentTimeMillis();

        for (int i = 0; i < 5; i++) {
            Future future = new FixedLengthFutureImpl(context);
            future.write(param);
            session.flush(future);
            ThreadUtil.sleep(300);
        }

        System.out.println("Time:" + (System.currentTimeMillis() - old));

        Thread.sleep(2000);

        CloseUtil.close(connector);

    }
}
