package cn.markrobot.handler;

public class EchoChannelHandler implements ChannelHandler {

    @Override
    public byte[] handle(byte[] bytes) {
        System.out.println("echo String: " + new String(bytes));
        return new byte[0];
    }

    @Override
    public byte[] exceptionCaught(Throwable cause) {
        return new byte[0];
    }
}
