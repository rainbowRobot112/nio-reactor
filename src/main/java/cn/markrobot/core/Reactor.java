package cn.markrobot.core;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;

public class Reactor implements Runnable {

    private Selector selector;
    private final ChannelRegister channelRegister;
    private ChannelAcceptor channelAcceptor;
    private volatile boolean shutdown;

    public Reactor(ChannelRegister channelRegister) {
        this.channelRegister = channelRegister;
    }

    public void asyncRun() {
        channelRegister.getExecutor().execute(this);
    }

    @Override
    public void run() {
        try {
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() throws IOException {
        selector = SelectorProvider.provider().openSelector();
        channelAcceptor = channelRegister.createAndRegisterAcceptor(selector);

        try {
            while (!Thread.interrupted() && !isShutdown()) {
                if (selector.select() > 0) {
                    selector.selectedKeys().removeIf(selectionKey -> {
                        dispatch(selectionKey);
                        return true;
                    });
                }
            }
        } finally {
            selector.wakeup();
            for (SelectionKey key : selector.keys()) {
                key.channel().close();
            }
            selector.close();
            channelAcceptor.stop();
        }
    }

    private void dispatch(SelectionKey key) {
        if (key.attachment() != null) {
            ((ChannelAcceptor) key.attachment()).start();
        }
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public void stop() {
        System.out.println("Reactor begin to stop");
        shutdown = true;
    }
}
