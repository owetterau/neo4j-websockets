package de.oliverwetterau.neo4j.websockets.client.server;

/**
 * This interface shall be used to inform the client about websocket connection status changes.
 *
 * @author Oliver Wetterau
 * @version 2015-09-01
 */
public interface ConnectionListener {
    /**
     * Called after a websocket connection to the server has been closed.
     */
    void onConnectionClosed();
}
