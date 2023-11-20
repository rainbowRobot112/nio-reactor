package cn.markrobot;

import cn.markrobot.core.Reactor;
import cn.markrobot.core.ReactorFactory;
import cn.markrobot.handler.EchoChannelHandler;

public class Server {

    public static void main(String[] args) throws Exception {
        Reactor reactor = new ReactorFactory.Builder()
                .handler(new EchoChannelHandler())
                .port(9900)
                .build()
                .creat();
        reactor.start();
    }
}
