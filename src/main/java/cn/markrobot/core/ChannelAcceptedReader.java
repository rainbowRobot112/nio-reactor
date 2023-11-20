package cn.markrobot.core;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;
import java.util.concurrent.Executor;

public class ChannelAcceptedReader implements Runnable {

    private final Executor executor;
    private final Selector selector;
    private volatile boolean shutdown;

    public ChannelAcceptedReader(Executor executor) throws IOException {
        selector = SelectorProvider.provider().openSelector();
        this.executor = executor;
    }

    public void register(SocketChannel channel, ChannelHandlerComposite handler) throws IOException {
        channel.register(selector, SelectionKey.OP_READ, handler);
        System.out.println("worker channel register. remote ip: " + channel.getRemoteAddress());
    }

    @Override
    public void run() {
        try {
            while (!shutdown) {
                try {
                    // 保证 CPU 不因为死循环被占满
                    if (selector.select(1L) > 0) {
                        Set<SelectionKey> keys = selector.selectedKeys();
                        keys.removeIf(selectionKey -> {
                            if (!selectionKey.isValid()) {
                                System.out.println("Discard inValid SelectionKey");
                                selectionKey.cancel();
                                return true;
                            }
                            if (selectionKey.isReadable() || selectionKey.isWritable()) {
                                executor.execute(new ChannelWorker(selectionKey));
                                return true;
                            }
                            return true;
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            try {
                selector.wakeup();
                for (SelectionKey key : selector.keys()) {
                    key.channel().close();
                }
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void stop() {
        shutdown = true;
    }
}
