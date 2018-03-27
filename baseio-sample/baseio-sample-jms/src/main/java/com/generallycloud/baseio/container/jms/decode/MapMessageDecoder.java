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
package com.generallycloud.baseio.container.jms.decode;

import com.alibaba.fastjson.JSONObject;
import com.generallycloud.baseio.codec.protobase.future.ParamedProtobaseFuture;
import com.generallycloud.baseio.component.Parameters;
import com.generallycloud.baseio.container.jms.MapMessage;
import com.generallycloud.baseio.container.jms.Message;

public class MapMessageDecoder implements MessageDecoder {

    @Override
    public Message decode(ParamedProtobaseFuture future) {
        Parameters param = future.getParameters();
        String messageId = param.getParameter("msgId");
        String queueName = param.getParameter("queueName");
        JSONObject map = param.getJSONObject("map");
        return new MapMessage(messageId, queueName, map);
    }
}
