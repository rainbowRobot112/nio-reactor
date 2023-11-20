package cn.markrobot.core;

import cn.markrobot.handler.ChannelHandler;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

public class ChannelRegister {

    private final ExecutorService executor;
    private final ChannelHandler channelHandler;
    private final int port;

    public ChannelRegister(int port, ExecutorService executor, ChannelHandler channelHandler) {
        this.port = port;
        this.executor = executor;
        this.channelHandler = channelHandler;
    }

    public ChannelAcceptedReader createAndRegisterWorker(SocketChannel channel) throws IOException {
        ChannelAcceptedReader reader = new ChannelAcceptedReader(executor);
        registerWorker(channel, reader);
        return reader;
    }

    public void registerWorker(SocketChannel channel, ChannelAcceptedReader reader) throws IOException{
        reader.register(channel, new ChannelHandlerComposite(channelHandler));
    }

    public ChannelAcceptor createAndRegisterAcceptor(Selector selector) throws IOException {
        ChannelAcceptor channelAcceptor = new ChannelAcceptor(this);
        channelAcceptor.register(selector, port);
        return channelAcceptor;
    }
    public ExecutorService getExecutor() {
        return executor;
    }

    public void stop() {
        executor.shutdown();
    }
}
