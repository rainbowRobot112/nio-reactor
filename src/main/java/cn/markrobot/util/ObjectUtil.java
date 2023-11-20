package cn.markrobot.util;

public class ObjectUtil {

    private ObjectUtil() {

    }

    public static <T> void checkNotNull(T arg, String text) {
        if (arg == null) {
            throw new NullPointerException(text);
        }
    }
}
