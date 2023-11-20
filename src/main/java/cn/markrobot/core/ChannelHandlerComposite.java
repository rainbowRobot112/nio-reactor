package cn.markrobot.core;

import cn.markrobot.handler.ChannelHandler;
import cn.markrobot.util.ChannelUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ChannelHandlerComposite {

    private final ChannelHandler channelHandler;

    private static final int LENGTH_BYTE = 4;
    private final ByteBuffer lengthByteBuffer = ByteBuffer.allocate(LENGTH_BYTE);
    private ByteBuffer messageByteBuffer;
    private ByteBuffer remainingWriteBuffer;

    public ChannelHandlerComposite(ChannelHandler handler) {
        this.channelHandler = handler;
    }

    public byte[] process(byte[] data) {
        return channelHandler.handle(data);
    }

    public byte[] decode(SocketChannel channel) throws IOException {
        return readReadyData(channel);
    }

    public byte[] encode(byte[] data) {
        int size = data.length;
        ByteBuffer buffer = ByteBuffer.allocate(ChannelUtil.DATA_LENGTH_BIT + size);
        buffer.putInt(size);
        buffer.put(data);
        return buffer.array();
    }

    public byte[] exceptionCaught(Throwable cause) {
        return channelHandler.exceptionCaught(cause);
    }

    private byte[] readReadyData(SocketChannel channel) throws IOException {
        if (checkAndCreateMessageByteBuffer(channel)) {
            int read = ChannelUtil.readChannel(channel, messageByteBuffer);
            if (read > 0 && isByteBufferFull(messageByteBuffer)) {
                messageByteBuffer.flip();
                byte[] bytes = messageByteBuffer.array();
                messageByteBuffer.clear();
                messageByteBuffer = null;
                lengthByteBuffer.clear();
                return bytes;
            }
        }
        return null;
    }

    private boolean checkAndCreateMessageByteBuffer(SocketChannel channel) throws IOException {
        if (messageByteBuffer == null) {
            if (!isByteBufferFull(lengthByteBuffer)) {
                ChannelUtil.readChannel(channel, lengthByteBuffer);
            }
            if (!isByteBufferFull(lengthByteBuffer)) {
                return false;
            }
            lengthByteBuffer.flip();
            messageByteBuffer = ByteBuffer.allocate(lengthByteBuffer.getInt());
        }
        return true;
    }

    private boolean isByteBufferFull(ByteBuffer buffer) {
        return buffer.capacity() == buffer.position();
    }

    public ByteBuffer getRemainingWriteBuffer() {
        return remainingWriteBuffer;
    }

    public void setRemainingWriteBuffer(ByteBuffer remainingWriteBuffer) {
        this.remainingWriteBuffer = remainingWriteBuffer;
    }

    public void removeRemainingWriteBuffer() {
        remainingWriteBuffer = null;
    }
}
