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
package com.generallycloud.baseio.container.jms.server;

import com.generallycloud.baseio.codec.protobase.future.ParamedProtobaseFuture;
import com.generallycloud.baseio.component.SocketSession;

public class MQConsumerServlet extends MQServlet {

    public static final String SERVICE_NAME = MQConsumerServlet.class.getSimpleName();

    @Override
    public void doAccept(SocketSession session, ParamedProtobaseFuture future,
            MQSessionAttachment attachment) throws Exception {

        getMQContext().pollMessage(session, future, attachment);
    }

}
