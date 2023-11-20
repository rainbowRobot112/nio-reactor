package cn.markrobot.core;

import cn.markrobot.util.ChannelUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ChannelWorker implements Runnable {

    private final SelectionKey key;

    public ChannelWorker(SelectionKey key) {
        this.key = key;
    }

    @Override
    public void run() {
        if (key.attachment() == null) {
            System.out.println("Not found attach in SelectionKey");
            return;
        }
        SocketChannel channel = (SocketChannel) key.channel();
        ChannelHandlerComposite worker = (ChannelHandlerComposite) key.attachment();
        if (key.isReadable()) {
            // 暂停监听, 进行读操作
            key.interestOps(0);
            byte[] writeBytes;
            try {
                byte[] data = worker.decode(channel);
                // -1, 表示连接已经断开
                if (worker.getLastReadSize() == -1) {
                    System.out.println("Connection down, remoteIp: " + channel.getRemoteAddress());
                    channel.close();
                    return;
                }
                if (data == null) {
                    // 还没读完, 等待下次 select 再继续读数据
                    key.interestOps(SelectionKey.OP_READ);
                    return;
                }
                byte[] response = worker.process(data);
                writeBytes = worker.encode(response);
                // 数据处理完成允许进入写状态
                key.interestOps(SelectionKey.OP_WRITE);
            } catch (Exception e) {
                writeBytes = worker.encode(worker.exceptionCaught(e));
            }
            try {
                ByteBuffer buffer = ByteBuffer.wrap(writeBytes);
                int totalWrite = ChannelUtil.writeChannel(channel, buffer);
                System.out.printf("Channel write bytes total: %d, buffer length: %d", totalWrite, writeBytes.length);
                if (totalWrite == 0) {
                    // 等待下次监听到写事件再写入
                    worker.setRemainingWriteBuffer(buffer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (key.isWritable()) {
            ByteBuffer remainingWriteBuffer = worker.getRemainingWriteBuffer();
            if (remainingWriteBuffer != null) {
                try {
                    int totalWrite = ChannelUtil.writeChannel((SocketChannel) key.channel(), remainingWriteBuffer);
                    System.out.printf("Channel write bytes total: %d, buffer length: %d", totalWrite, remainingWriteBuffer.limit());
                    if (totalWrite == remainingWriteBuffer.limit()) {
                        // 写操作完成, 关闭通道
                        key.cancel();
                        worker.removeRemainingWriteBuffer();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
