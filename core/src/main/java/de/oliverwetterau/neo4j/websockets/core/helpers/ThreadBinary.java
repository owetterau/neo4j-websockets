package de.oliverwetterau.neo4j.websockets.core.helpers;

public class ThreadBinary {
    protected static ThreadLocal<Boolean> threadLocal = new ThreadLocal<>();
    protected static Boolean fixedBinary = null;

    public static void setFixedBinary(boolean isBinary) {
        fixedBinary = isBinary;
    }

    public static void setBinary(boolean isBinary) {
        threadLocal.set(isBinary);
    }

    public static Boolean isBinary() {
        if (fixedBinary != null) {
            return fixedBinary;
        }

        Boolean isBinary = threadLocal.get();

        return (isBinary == null) ? false : isBinary;
    }
}
