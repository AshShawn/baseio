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
package com.generallycloud.baseio.component;

import com.generallycloud.baseio.concurrent.ExecutorEventLoopGroup;
import com.generallycloud.baseio.concurrent.FixedAtomicInteger;
import com.generallycloud.baseio.concurrent.LineEventLoopGroup;
import com.generallycloud.baseio.concurrent.ThreadEventLoopGroup;
import com.generallycloud.baseio.configuration.ServerConfiguration;

public class NioSocketChannelContext extends AbstractSocketChannelContext {

    private FixedAtomicInteger            channelIds;

    private NioChannelService             channelService;

    private NioGlobalSocketSessionManager sessionManager;

    public NioSocketChannelContext(ServerConfiguration configuration) {
        super(configuration);
        this.sessionManager = new NioGlobalSocketSessionManager();
    }

    private FixedAtomicInteger createChannelIdsSequence() {
        int core_size = getServerConfiguration().getSERVER_CORE_SIZE();
        int max = (Integer.MAX_VALUE / core_size) * core_size - 1;
        return new FixedAtomicInteger(0, max);
    }

    @Override
    protected ExecutorEventLoopGroup createExecutorEventLoopGroup() {
        int eventLoopSize = getServerConfiguration().getSERVER_CORE_SIZE();
        if (getServerConfiguration().isSERVER_ENABLE_WORK_EVENT_LOOP()) {
            return new ThreadEventLoopGroup(this, "event-process", eventLoopSize);
        } else {
            return new LineEventLoopGroup("event-process", eventLoopSize);
        }
    }

    @Override
    protected void doStartModule() throws Exception {
        channelIds = createChannelIdsSequence();
        super.doStartModule();
    }

    public FixedAtomicInteger getChannelIds() {
        return channelIds;
    }
    
    @Override
    public NioChannelService getChannelService() {
        return channelService;
    }

    @Override
    public NioGlobalSocketSessionManager getSessionManager() {
        return sessionManager;
    }

    @Override
    public void setChannelService(ChannelService service) {
        this.channelService = (NioChannelService) service;
    }

}
