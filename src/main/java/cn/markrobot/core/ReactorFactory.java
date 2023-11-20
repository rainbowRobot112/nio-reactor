package cn.markrobot.core;

import cn.markrobot.executor.WorkerThreadFactory;
import cn.markrobot.handler.ChannelHandler;
import cn.markrobot.util.ObjectUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ReactorFactory {

    private final ExecutorService executor;
    private final ChannelHandler channelHandler;
    private final int port;

    private static final int DEFAULT_WORKER_THREAD_NUMBER = 32;

    public ReactorFactory(Builder builder) {
        port = builder.port;
        int workThreadNum = builder.workThreadNum;
        if (workThreadNum == -1) {
            workThreadNum = DEFAULT_WORKER_THREAD_NUMBER;
        }
        channelHandler = builder.channelHandler;
        // 提前创建工作线程池
        executor = createExecutor(workThreadNum);
    }

    protected ExecutorService createExecutor(int workThreadNum) {
        return new ThreadPoolExecutor(workThreadNum,
                workThreadNum,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new WorkerThreadFactory("Reactor-worker-pool-thread-"),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public Reactor creat() {
        return new Reactor(new ChannelRegister(port, executor, channelHandler));
    }

    public static class Builder {
        private int port;
        private int workThreadNum = -1;
        private ChannelHandler channelHandler;

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder setWorkThreadNum(int num) {
            this.workThreadNum = num;
            return this;
        }

        public Builder handler(ChannelHandler channelHandler) {
            this.channelHandler = channelHandler;
            return this;
        }

        public ReactorFactory build() {
            ObjectUtil.checkNotNull(channelHandler, "ChannelHandler Not Found");
            return new ReactorFactory(this);
        }
    }
}
