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
package com.generallycloud.baseio.codec.protobase;

import java.io.IOException;

import com.generallycloud.baseio.buffer.ByteBuf;
import com.generallycloud.baseio.codec.protobase.future.HashedProtobaseBinaryFutureImpl;
import com.generallycloud.baseio.codec.protobase.future.HashedProtobaseFutureImpl;
import com.generallycloud.baseio.component.SocketChannel;
import com.generallycloud.baseio.protocol.ChannelFuture;

/**
 * <pre>
 * 
 *  B0：
 *  +---------------------------------------------------------------------+
 *  |                                                                     |
 *  +      -       -       -       -       -       -       -       -      +
 *  |      0       1       2       3       4       5       6       7      | 
 *  +      -       -       -       -       -       -       -       -      +
 *  |       message    |  has  |   is    |                                |
 *  |        type      | binary|broadcast|                                |
 *  +---------------------------------------------------------------------+
 *  
 *  B0:0-1	: 报文类型 [0=UNKONW,1=PACKET,2=BEAT.PING,3=BEAT.PONG]
 *  B0:2  	: 推送类型 [0=PUSH,1=BRODCAST]
 *  B0:3		: 是否带有二进制数据[1=true,0=false]
 *  B0:4-7	: 预留
 *  B1		: future   name  length
 *  B4	 	: future   id
 *  B4	 	: session  id
 *  B4	 	: hash     code
 *  B2	 	：text           length
 *  B4	 	：binary         length //FIXME 是否应该设置为两字节？
 * 
 * </pre>
 */
public class HashedProtobaseProtocolDecoder extends ProtobaseProtocolDecoder {

    public static final int PROTOCOL_HEADER_WITHBINARY = 20;
    public static final int PROTOCOL_HEADER_NO_BINARY  = 16;

    public HashedProtobaseProtocolDecoder(int limit) {
        super(limit);
    }

    @Override
    protected ChannelFuture newChannelReadFutureNoBinary(SocketChannel channel, ByteBuf temporary,
            byte b) throws IOException {
        ByteBuf buf = temporary.limit(PROTOCOL_HEADER_NO_BINARY - 1);
        return new HashedProtobaseFutureImpl(channel, buf, isBroadcast(b));
    }

    @Override
    protected ChannelFuture newChannelReadFutureWithBinary(SocketChannel channel, ByteBuf temporary,
            byte b) throws IOException {
        ByteBuf buf = temporary.limit(PROTOCOL_HEADER_WITHBINARY - 1);
        return new HashedProtobaseBinaryFutureImpl(channel, buf, limit, isBroadcast(b));
    }

}
