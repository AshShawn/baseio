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

import java.io.IOException;

import com.generallycloud.baseio.common.CloseUtil;

/**
 * @author wangkai
 *
 */
public class CloseSelectorLoopEvent implements SelectorLoopEvent {

    private boolean           closed;

    private SelectorLoopEvent event;

    public CloseSelectorLoopEvent(SelectorLoopEvent event) {
        this.event = event;
    }

    @Override
    public void close() throws IOException {}

    @Override
    public void fireEvent(SocketSelectorEventLoop selectorLoop) throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        CloseUtil.close(event);
    }

}
