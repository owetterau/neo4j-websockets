package de.oliverwetterau.neo4j.websockets.core.data;

/**
 * This class holds a list of parameter names being used in json messages which are sent between client and server.
 *
 * @author Oliver Wetterau
 * @version 2015-09-01
 */
public class CommandParameters {
    public static final String SERVICE = "s";
    public static final String METHOD = "m";
    public static final String PARAMETERS = "p";

    public static final String COUNTRY = "c";
    public static final String LANGUAGE = "l";
}
