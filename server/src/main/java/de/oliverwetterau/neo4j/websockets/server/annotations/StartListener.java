package de.oliverwetterau.neo4j.websockets.server.annotations;

import io.undertow.Undertow;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * Created by oliver on 15.11.15.
 */
public interface StartListener {
    public void onStart(GraphDatabaseService graphDatabaseService, Undertow undertow);
}
