package de.oliverwetterau.neo4j.websockets.server.neo4j;

import org.neo4j.graphdb.config.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by oliver on 27.08.15.
 */
public class EmbeddedNeo4jBuilder {
    protected static final Logger logger = LoggerFactory.getLogger(EmbeddedNeo4jBuilder.class);

    protected static Class embeddedNeo4jClass;
    protected static Configurer configurer;
    protected static Map<Setting,String> settings;

    protected static EmbeddedNeo4j embeddedNeo4j;

    public static void init(final Class embeddedNeo4jClass, final Configurer configurer, final Map<Setting,String> settings) {
        assert EmbeddedNeo4j.class.isAssignableFrom(embeddedNeo4jClass);

        EmbeddedNeo4jBuilder.embeddedNeo4jClass = embeddedNeo4jClass;
        EmbeddedNeo4jBuilder.configurer = configurer;
        EmbeddedNeo4jBuilder.settings = settings;

        configurer.init(settings);
    }

    public static EmbeddedNeo4j getNeo4j() {
        if (embeddedNeo4j != null) {
            return embeddedNeo4j;
        }

        try {
            embeddedNeo4j = (EmbeddedNeo4j) embeddedNeo4jClass.newInstance();
        }
        catch (InstantiationException | IllegalAccessException e) {
            logger.error("[getNeo4j] could not create EmbeddedNeo4j instance");
            return null;
        }

        embeddedNeo4j.initialize(configurer);

        return embeddedNeo4j;
    }
}
