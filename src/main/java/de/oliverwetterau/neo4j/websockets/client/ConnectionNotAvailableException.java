package de.oliverwetterau.neo4j.websockets.client;

/**
 * This exception will be thrown when a server is not available.
 *
 * @author Oliver Wetterau
 * @version 2015-09-01
 */
public class ConnectionNotAvailableException extends Exception {
    protected Server server;

    public ConnectionNotAvailableException(Server server) {
        super();
        this.server = server;
    }
}
