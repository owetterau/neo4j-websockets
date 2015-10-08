package de.oliverwetterau.neo4j.websockets.server.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.kernel.ha.HighlyAvailableGraphDatabase;
import org.neo4j.server.configuration.ServerConfigurator;

import java.util.Map;

/**
 * This is interface is used for the creation of Neo4j instances.
 *
 * @author Oliver Wetterau
 */
public interface Configurer {
    /**
     * Sets environment parameters for the to be created Neo4j instance.
     * @param settings Neo4j server settings
     */
    void init(final Map<Setting, String> settings);

    /**
     * Gets a Neo4j instance based on the parameters used during initialisation. If the database instance does not exist
     * yet, a new instance will be created.
     * @return Ne4oj instance
     */
    GraphDatabaseService getGraphDatabase();

    /**
     * Shuts down the Neo4j database instance.
     */
    void dropGraphDatabase();

    /**
     * Gets a Neo4j server configurator based on the instances used during initialisation.
     * @param databaseService database service the server configurator shall use
     * @return Neo4j server configurator
     */
    ServerConfigurator getServerConfigurator(final HighlyAvailableGraphDatabase databaseService);

    /**
     * Gets the internal high availability id in a cluster.
     * @return high availability id in a cluster
     */
    Integer getHighAvailabilityId();

    /**
     * Gets whether database instance is a productive, permanent database or not.
     * @return true: production database, false: test database
     */
    boolean isProductionDatabase();
}
