package de.oliverwetterau.neo4j.websockets.client.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.oliverwetterau.neo4j.websockets.client.ApplicationSettings;
import de.oliverwetterau.neo4j.websockets.client.web.DataConnection;
import de.oliverwetterau.neo4j.websockets.client.web.ManagementConnection;
import de.oliverwetterau.neo4j.websockets.core.data.CommandParameters;
import de.oliverwetterau.neo4j.websockets.core.data.json.JsonObjectMapper;
import de.oliverwetterau.neo4j.websockets.core.i18n.ThreadLocale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Holds information about a Neo4j server and manages available and used connections to this server.
 *
 * @author Oliver Wetterau
 * @version 2015-09-01
 */
public class Server implements Comparable<Server> {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    protected static JsonObjectMapper jsonObjectMapper;

    /** language settings */
    protected final ThreadLocale threadLocale;
    /** uri to be used for management connection */
    protected final String managementUri;
    /** uri to be used for data connections */
    protected final String dataUri;
    /** Neo4j cluster id */
    protected String id = "";
    /** is this server a master / write server? */
    protected boolean isMaster;

    /** list of all available and free data connections to this server */
    protected final Queue<DataConnection> availableConnections = new ConcurrentLinkedQueue<>();
    /** list of all data connections to this server currently in use */
    protected final Set<DataConnection> usedConnections = new ConcurrentSkipListSet<>();
    /** management connection to this server */
    protected final ManagementConnection managementConnection;

    /** binary or text web socket connection? */
    protected final boolean isBinary;

    /**
     * Constructor
     * @param clusterListener listener that will be informed about changes in the cluster configuration
     * @param uri base uri of the Neo4j server
     * @param threadLocale language settings
     * @param isBinary use binary or text format?
     */
    public Server(final ClusterListener clusterListener, final String uri, final ThreadLocale threadLocale, final boolean isBinary) {
        this.managementUri = uri + ApplicationSettings.managementPath();
        this.dataUri = uri + ApplicationSettings.dataPath();
        this.threadLocale = threadLocale;
        this.isBinary = isBinary;

        this.managementConnection = new ManagementConnection(clusterListener, managementUri);
    }

    /**
     * Sets the wrapper for a json object mapper
     * @param jsonObjectMapper wrapper for a json object mapper
     */
    public static void setJsonObjectMapper(JsonObjectMapper jsonObjectMapper) {
        Server.jsonObjectMapper = jsonObjectMapper;
    }

    /**
     * Gets the Neo4j cluster id.
     * @return Neo4j cluster id
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the full Neo4j server uri for the management connection.
     * @return Neo4j server uri for management connection
     */
    public String getManagementUri() {
        return managementUri;
    }

    /**
     * Gets the full Neo4j server uri for the data connection.
     * @return Neo4j server uri for data connection
     */
    public String getDataUri() {
        return dataUri;
    }

    /**
     * Tries to establish a management connection the Neo4j server.
     * @throws Exception exception
     */
    public void connect() throws Exception {
        managementConnection.connect();

        if (id.length() == 0) {
            register();
        }
    }

    /**
     * Registers with the Neo4j server and saves some Neo4j server information with this object.
     */
    public void register() {
        logger.debug("[register] uri = {}", getManagementUri());

        JsonNode jsonNode;
        ObjectMapper objectMapper = jsonObjectMapper.getObjectMapperBinary();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put(CommandParameters.METHOD, "register");

        try {
            jsonNode = objectMapper.readTree(managementConnection.sendWithResult(objectMapper.writeValueAsBytes(objectNode)));
        }
        catch (Exception e) {
            logger.error("[register] could not register at server");
            return;
        }

        id = jsonNode.get("id").asText();
        setMaster(jsonNode.get("isMaster").asBoolean());

        managementConnection.setServerId(id);

        logger.debug("[register] id = {}, isMaster = {}, uri = {}", getId(), isMaster(), getManagementUri());
    }

    /**
     * Sets the availability status of this server.
     * @param isAvailable availability status
     */
    public void setAvailable(boolean isAvailable) {
        managementConnection.setAvailable(isAvailable);
    }

    /**
     * Gets the availability status of this server.
     * @return availability status
     */
    public boolean isAvailable() {
        return managementConnection.isAvailable();
    }

    /**
     * Sets whether this server is a master / write server.
     * @param isMaster is server a mster / write server?
     */
    public void setMaster(boolean isMaster) {
        this.isMaster = isMaster;
    }

    /**
     * Gets whether this server is a master / write server.
     * @return is server a mster / write server?
     */
    public boolean isMaster() {
        return isMaster;
    }

    /**
     * Gets a connection from the pool of available and free data connections to this server.
     * @return data connection to this server
     */
    public DataConnection getConnection() {
        DataConnection connection;

        for (connection = availableConnections.poll();
             connection != null && !connection.isUsable();
             connection = availableConnections.poll())
        {
            if (!connection.isUsable()) {
                connection.close();
            }
        }

        if (connection == null) {
            connection = new DataConnection(getDataUri());

            try {
                connection.connect();
            }
            catch (Exception e) {
                logger.error("[getConnection] could not connect to database", e);
                return null;
            }
        }

        connection.setLocale(threadLocale.getLocale());

        return connection;
    }

    /**
     * Returns a connection to the pool of available and free data connections to this server.
     * @param connection data connection to this server
     */
    public void returnConnection(DataConnection connection) {
        if (connection.isUsable()) {
            connection.setLastUsage(new Date());
            availableConnections.add(connection);
        }
        else {
            connection.close();
        }

        usedConnections.remove(connection);
    }

    /**
     * Compares two servers using their Neo4j cluster ids.
     * @param o other server to compare with
     * @return comparison result
     */
    @Override
    public int compareTo(final Server o) {
        return managementUri.compareTo(o.managementUri);
    }
}
