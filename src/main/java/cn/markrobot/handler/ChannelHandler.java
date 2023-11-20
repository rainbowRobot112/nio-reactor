package cn.markrobot.handler;

public interface ChannelHandler {

    byte[] handle(byte[] bytes);
    byte[] exceptionCaught(Throwable cause);
}
