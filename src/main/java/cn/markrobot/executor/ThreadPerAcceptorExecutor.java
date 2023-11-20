package cn.markrobot.executor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

public class ThreadPerAcceptorExecutor implements Executor {

    private final ThreadFactory threadFactory;

    public ThreadPerAcceptorExecutor(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        this.threadFactory = threadFactory;
    }

    @Override
    public void execute(Runnable command) {
        threadFactory.newThread(command).start();
    }
}
