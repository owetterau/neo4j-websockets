package de.oliverwetterau.neo4j.websockets.server;

import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by oliver on 15.11.15.
 */
@Configuration
public class DatabaseConfiguration {
    private static GraphDatabaseService graphDatabaseService;

    @Bean
    public GraphDatabaseService getGraphDatabaseService() {
        return graphDatabaseService;
    }

    protected static void setGraphDatabaseService(GraphDatabaseService graphDatabaseService) {
        DatabaseConfiguration.graphDatabaseService = graphDatabaseService;
    }
}
