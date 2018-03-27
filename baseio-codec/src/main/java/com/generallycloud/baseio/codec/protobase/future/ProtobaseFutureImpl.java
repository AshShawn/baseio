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
package com.generallycloud.baseio.codec.protobase.future;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import com.generallycloud.baseio.balance.BalanceFuture;
import com.generallycloud.baseio.buffer.ByteBuf;
import com.generallycloud.baseio.common.StringUtil;
import com.generallycloud.baseio.component.SocketChannel;
import com.generallycloud.baseio.component.SocketChannelContext;
import com.generallycloud.baseio.protocol.AbstractChannelFuture;

/**
 *
 */
public class ProtobaseFutureImpl extends AbstractChannelFuture implements ProtobaseFuture {

    private int     binaryLengthLimit;
    private byte[]  binaryReadBuffer;
    private int     binaryReadSize;
    private byte[]  binaryWriteBuffer;
    private int     binaryWriteSize;
    private boolean body_complete;
    private int     futureId;
    private String  futureName;
    private byte    futureNameLength;
    private int     hashCode;
    private boolean header_complete;
    private boolean isBroadcast;
    private int     sessionId;
    private int     textLength;
    private int     textLengthLimit;

    public ProtobaseFutureImpl(SocketChannel channel, ByteBuf buf) {
        super(channel.getContext());
        this.buf = buf;
    }

    // for ping & pong
    public ProtobaseFutureImpl(SocketChannelContext context) {
        super(context);
        this.header_complete = true;
        this.body_complete = true;
    }

    public ProtobaseFutureImpl(SocketChannelContext context, int futureId, String futureName) {
        super(context);
        this.futureName = futureName;
        this.futureId = futureId;
    }

    public ProtobaseFutureImpl(SocketChannelContext context, String futureName) {
        this(context, 0, futureName);
    }

    @Override
    public int getFutureId() {
        return futureId;
    }

    @Override
    public String getFutureName() {
        return futureName;
    }

    @Override
    public int getHashCode() {
        return hashCode;
    }

    @Override
    public byte[] getReadBinary() {
        return binaryReadBuffer;
    }

    @Override
    public int getReadBinarySize() {
        return binaryReadSize;
    }

    @Override
    public int getSessionId() {
        return sessionId;
    }

    @Override
    public int getSessionKey() {
        return sessionId;
    }

    @Override
    public int getTextLength() {
        return textLength;
    }

    @Override
    public byte[] getWriteBinary() {
        return binaryWriteBuffer;
    }

    @Override
    public int getWriteBinarySize() {
        return binaryWriteSize;
    }

    @Override
    public boolean hasReadBinary() {
        return binaryReadSize > 0;
    }

    @Override
    public boolean isBroadcast() {
        return isBroadcast;
    }

    @Override
    public boolean read(SocketChannel channel, ByteBuf buffer) throws IOException {
        ByteBuf buf = this.buf;
        if (!header_complete) {
            buf.read(buffer);
            if (buf.hasRemaining()) {
                return false;
            }
            if (futureNameLength == 0) {
                int nextLen = 4;
                byte h1 = buf.getByte(0);
                int type = h1 & 0b11000000;
                if (type > 0b01000000) {
                    setHeartbeat(type == 0b10000000);
                    return true;
                }
                isBroadcast = ((h1 & 0b00100000) != 0);
                if ((h1 & 0b00010000) != 0) {
                    futureId = -1;
                    nextLen += 4;
                }
                if ((h1 & 0b00001000) != 0) {
                    sessionId = -1;
                    nextLen += 4;
                }
                if ((h1 & 0b00000100) != 0) {
                    hashCode = -1;
                    nextLen += 4;
                }
                if ((h1 & 0b00000010) != 0) {
                    binaryReadSize = -1;
                    nextLen += 4;
                }
                futureNameLength = buf.getByte(1);
                if (futureNameLength < 1) {
                    throw new IOException("futureNameLength < 1");
                }
                nextLen += futureNameLength;
                buf.reallocate(nextLen);
                return read(channel, buffer);
            }
            buf.flip();
            textLength = buf.getInt();
            if (futureId == -1) {
                futureId = buf.getInt();
            }
            if (sessionId == -1) {
                sessionId = buf.getInt();
            }
            if (hashCode == -1) {
                hashCode = buf.getInt();
            }
            if (binaryReadSize == -1) {
                binaryReadSize = buf.getInt();
            }
            Charset charset = context.getEncoding();
            ByteBuffer memory = buf.nioBuffer();
            futureName = StringUtil.decode(charset, memory);
            buf.reallocate(textLength + binaryReadSize);
            header_complete = true;
        }
        if (!body_complete) {
            buf.read(buffer);
            if (buf.hasRemaining()) {
                return false;
            }
            body_complete = true;
            if (textLength > 0) {
                buf.flip();
                buf.markPL();
                buf.limit(textLength);
                Charset charset = context.getEncoding();
                ByteBuffer memory = buf.nioBuffer();
                readText = StringUtil.decode(charset, memory);
                buf.reset();
                buf.skipBytes(textLength);
            }
            if (binaryReadSize > 0) {
                this.binaryReadBuffer = buf.getBytes();
            }
        }
        return true;
    }

    @Override
    public void setBroadcast(boolean broadcast) {
        this.isBroadcast = broadcast;
    }

    @Override
    public void setFutureId(int futureId) {
        this.futureId = futureId;
    }

    @Override
    public void setFutureName(String futureName) {
        this.futureName = futureName;
    }

    @Override
    public void setHashCode(int hashCode) {
        this.hashCode = hashCode;
    }

    @Override
    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return getFutureName() + "@" + getReadText();
    }

    @Override
    public BalanceFuture translate() {
        String text = getReadText();
        if (StringUtil.isNullOrBlank(text)) {
            return this;
        }
        write(text);
        return this;
    }

    @Override
    public void writeBinary(byte b) {
        if (binaryWriteBuffer == null) {
            binaryWriteBuffer = new byte[256];
        }
        int newcount = writeSize + 1;
        if (newcount > binaryWriteBuffer.length) {
            binaryWriteBuffer = Arrays.copyOf(binaryWriteBuffer, binaryWriteBuffer.length << 1);
        }
        binaryWriteBuffer[writeSize] = (byte) b;
        writeSize = newcount;
    }

    @Override
    public void writeBinary(byte[] bytes) {
        write(bytes, 0, bytes.length);
    }

    @Override
    public void writeBinary(byte[] bytes, int off, int len) {
        if (binaryWriteBuffer == null) {
            if ((len - off) != bytes.length) {
                binaryWriteBuffer = new byte[len];
                System.arraycopy(bytes, off, binaryWriteBuffer, 0, len);
                return;
            }
            binaryWriteBuffer = bytes;
            writeSize = len;
            return;
        }
        int newcount = writeSize + len;
        if (newcount > binaryWriteBuffer.length) {
            binaryWriteBuffer = Arrays.copyOf(binaryWriteBuffer,
                    Math.max(binaryWriteBuffer.length << 1, newcount));
        }
        System.arraycopy(bytes, off, binaryWriteBuffer, writeSize, len);
        writeSize = newcount;
    }

}
