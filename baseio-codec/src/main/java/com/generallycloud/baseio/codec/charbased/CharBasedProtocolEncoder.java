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
package com.generallycloud.baseio.codec.charbased;

import java.io.IOException;

import com.generallycloud.baseio.buffer.ByteBuf;
import com.generallycloud.baseio.buffer.ByteBufAllocator;
import com.generallycloud.baseio.codec.charbased.future.CharBasedFuture;
import com.generallycloud.baseio.protocol.ChannelFuture;
import com.generallycloud.baseio.protocol.ProtocolEncoder;

public class CharBasedProtocolEncoder implements ProtocolEncoder {

    private byte splitor;

    public CharBasedProtocolEncoder(byte splitor) {
        this.splitor = splitor;
    }

    @Override
    public void encode(ByteBufAllocator allocator, ChannelFuture future) throws IOException {

        CharBasedFuture f = (CharBasedFuture) future;

        int writeSize = f.getWriteSize();

        if (writeSize == 0) {
            throw new IOException("null write buffer");
        }

        ByteBuf buf = allocator.allocate(writeSize + 1);

        buf.put(f.getWriteBuffer(), 0, writeSize);

        buf.putByte(splitor);

        future.setByteBuf(buf.flip());
    }

}
