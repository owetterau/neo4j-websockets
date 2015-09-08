package de.oliverwetterau.neo4j.websockets.client;

/**
 * Created by oliver on 01.09.15.
 */
public class ConnectionNotAvailableException extends Exception {
    protected Server server;

    public ConnectionNotAvailableException(Server server) {
        super();
        this.server = server;
    }
}
