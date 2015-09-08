package de.oliverwetterau.neo4j.websockets.core.helpers;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by oliver on 26.08.15.
 */
public class ExceptionConverter {
    public static String toString(final Throwable e) {

        return e.getMessage() + "\n=> " + stackTrace(e);
    }

    public static String stackTrace(final Throwable e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        printWriter.close();

        return stringWriter.toString();
    }
}
