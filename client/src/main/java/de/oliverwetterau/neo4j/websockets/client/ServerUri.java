package de.oliverwetterau.neo4j.websockets.client;

/**
 * Interface that must be implemented by a class to provide a list of Neo4j server uri's.
 *
 * @author Oliver Wetterau
 * @version 2015-09-01
 */
public interface ServerUri {
    String[] getServerUris();
}
