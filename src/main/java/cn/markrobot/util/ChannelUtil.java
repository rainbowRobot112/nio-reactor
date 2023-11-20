package cn.markrobot.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class ChannelUtil {

    public static final int DATA_LENGTH_BIT = 4;

    private ChannelUtil() {

    }

    public static int readChannel(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        int read = channel.read(buffer);
        if (read == -1) {
            channel.close();
        }
        return read;
    }

    public static int writeChannel(WritableByteChannel channel, ByteBuffer buffer) throws IOException {
        int total = 0;
        while (buffer.hasRemaining()) {
            int count = channel.write(buffer);
            if (count == 0) {
                return 0;
            }
            total += count;
        }
        return total;
    }
}
