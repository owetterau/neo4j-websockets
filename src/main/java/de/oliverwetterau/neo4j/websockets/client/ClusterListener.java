package de.oliverwetterau.neo4j.websockets.client;

/**
 * This interface shall be used to inform the client about Neo4j cluster availability changes.
 *
 * @author Oliver Wetterau
 * @version 2015-09-01
 */
public interface ClusterListener {
    /**
     * Called when a server in the cluster is available (again).
     * @param id cluster id
     * @param role server role (master / slave)
     */
    void onServerAvailable(final String id, final String role);

    /**
     * Called when a server in the cluster is not available (anymore).
     * @param Id cluster id
     */
    void onServerUnavailable(final String Id);

    /**
     * Called when the websocket connection to a server in the cluster was re-established.
     * @param id cluster id
     * @param uri websocket uri
     */
    void onServerReconnected(final String id, final String uri);
}
