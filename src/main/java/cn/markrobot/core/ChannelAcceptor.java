package cn.markrobot.core;

import cn.markrobot.executor.ThreadPerAcceptorExecutor;
import cn.markrobot.executor.WorkerThreadFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public class ChannelAcceptor {

    private final ChannelAcceptedReader[] channelAcceptedReaders;
    private ServerSocketChannel serverSocketChannel;
    private final Executor defaultExecutor;
    private final ChannelRegister channelRegister;
    private final AtomicInteger index = new AtomicInteger(0);
    private volatile boolean shutdown;

    private final static int SOCKET_TIMEOUT = 10000;

    public ChannelAcceptor(ChannelRegister channelRegister) {
        this.channelRegister = channelRegister;
        channelAcceptedReaders = new ChannelAcceptedReader[Runtime.getRuntime().availableProcessors() * 2];
        defaultExecutor = new ThreadPerAcceptorExecutor(new WorkerThreadFactory("Reactor-listener-pool-"));
    }

    public void register(Selector selector, int port) throws IOException {
        serverSocketChannel = SelectorProvider.provider().openServerSocketChannel();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, this);
    }

    public void start() {
        try {
            SocketChannel channel = serverSocketChannel.accept();
            if (channel != null && !shutdown) {
                channel.configureBlocking(false);
                setSocketOption(channel.socket());
                int index = next();
                ChannelAcceptedReader channelAcceptedReader = channelAcceptedReaders[index];
                if (channelAcceptedReader == null) {
                    synchronized (channelAcceptedReaders) {
                        channelAcceptedReader = channelAcceptedReaders[index];
                        if (channelAcceptedReader == null) {
                            channelAcceptedReader = channelRegister.createAndRegisterWorker(channel);
                            channelAcceptedReaders[index] = channelAcceptedReader;
                            defaultExecutor.execute(channelAcceptedReader);
                        }
                    }
                } else {
                    channelRegister.registerWorker(channel, channelAcceptedReader);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setSocketOption(Socket socket) throws SocketException {
        socket.setSoTimeout(SOCKET_TIMEOUT);
        socket.setTcpNoDelay(false);
    }

    public int next() {
        return Math.abs(index.getAndIncrement() % channelAcceptedReaders.length);
    }

    public void stop() {
        shutdown = true;
        for (ChannelAcceptedReader work : channelAcceptedReaders) {
            if (work != null) {
                work.stop();
            }
        }
        try {
            // 等待 2s, 无论 reader 是否处理完都关闭线程池
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            channelRegister.stop();
        }
    }
}
