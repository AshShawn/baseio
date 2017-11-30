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
package com.generallycloud.baseio.concurrent;

import java.util.concurrent.RejectedExecutionException;

public class AioLineEventLoop implements ExecutorEventLoop {

    private ExecutorEventLoopGroup eventLoopGroup;

    public AioLineEventLoop(ExecutorEventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = eventLoopGroup;
    }

    @Override
    public void dispatch(Runnable job) throws RejectedExecutionException {

        if (job == null) {
            return;
        }

        job.run();
    }

    @Override
    public Thread getMonitor() {
        return Thread.currentThread();
    }

    @Override
    public boolean inEventLoop() {
        return false;
    }

    @Override
    public boolean inEventLoop(Thread thread) {
        return getMonitor() == thread;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void startup(String threadName) throws Exception {}

    @Override
    public void loop() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop() {}

    @Override
    public void wakeup() {
    }

    @Override
    public ExecutorEventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

}
