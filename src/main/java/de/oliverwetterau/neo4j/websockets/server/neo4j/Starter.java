package de.oliverwetterau.neo4j.websockets.server.neo4j;

import org.neo4j.graphdb.config.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by oliver on 26.08.15.
 */
public class Starter {
    protected static final Logger logger = LoggerFactory.getLogger(Starter.class);
    protected static boolean isInitialized = false;

    protected final Class embeddedNeo4jClass;
    protected final Configurer configurer;
    protected final Map<Setting,String> settings;

    protected EmbeddedNeo4j embeddedNeo4j;

    public Starter(final Class embeddedNeo4jClass, final Configurer configurer, final Map<Setting,String> settings) {
        assert EmbeddedNeo4j.class.isAssignableFrom(embeddedNeo4jClass);

        this.embeddedNeo4jClass = embeddedNeo4jClass;
        this.configurer = configurer;
        this.settings = settings;

        EmbeddedNeo4jBuilder.init(embeddedNeo4jClass, configurer, settings);
    }

    /**
     * Run the initialization.
     * @throws Exception any exception that might appear
     */
    public void run() throws Exception {
        if (isInitialized) return;

        before(embeddedNeo4jClass);

        embeddedNeo4j = EmbeddedNeo4jBuilder.getNeo4j();

        if (embeddedNeo4j == null) {
            logger.error("[run] could not create EmbeddedNeo4j instance");
            throw new Exception("could not create EmbeddedNeo4j instance");
        }

        embeddedNeo4j.start();
        if (configurer.isProductionDatabase()) {
            embeddedNeo4j.setHighAvailabilityListener(new HighAvailabilityListener());
        }

        after(embeddedNeo4j);

        isInitialized = true;
    }

    public void before(final Class embeddedNeo4jClass) {
    }

    public void after(final EmbeddedNeo4j embeddedNeo4j) {
    }
}
