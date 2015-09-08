package de.oliverwetterau.neo4j.websockets.client;

import de.oliverwetterau.neo4j.websockets.client.helpers.ConcurrentSequence;
import de.oliverwetterau.neo4j.websockets.core.data.Error;
import de.oliverwetterau.neo4j.websockets.core.data.Result;
import de.oliverwetterau.neo4j.websockets.core.data.json.JsonObjectMapper;
import de.oliverwetterau.neo4j.websockets.core.i18n.ThreadLocale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Manages connections to a cluster of Neo4j servers, deals with load balances and provides functionality to send read
 * and write messages to Neo4j server using web sockets.
 *
 * @author Oliver Wetterau
 * @version 2015-09-01
 */
@Service
public class Database implements ClusterListener {
    private static final Logger logger = LoggerFactory.getLogger(Database.class);

    /** list all servers in the managed cluster */
    protected final Set<Server> SERVERS = new ConcurrentSkipListSet<>();
    /** multi threaded sequence used for load balancing of read servers */
    protected final ConcurrentSequence readSequence = new ConcurrentSequence();
    /** list of all servers currently acting as slaves / read servers */
    protected Server[] readServers;
    /** currently active master / write server */
    protected Server writeServer = null;

    /** Locale used in current thread */
    protected final ThreadLocale threadLocale;

    /**
     * Constructor
     * @param serverUri class which returns a list of Neo4j server URIs
     */
    @Autowired
    public Database(final ServerUri serverUri, final JsonObjectMapper jsonObjectMapper, final ThreadLocale threadLocale) {
        String[] uris = serverUri.getServerUris();
        Server server;
        Server masterServer = null;

        // initially set JsonObjectMapper in all classes that need it
        Server.setJsonObjectMapper(jsonObjectMapper);
        WebSocketHandler.setJsonObjectMapper(jsonObjectMapper);
        Result.setJsonObjectMapper(jsonObjectMapper);
        Error.setJsonObjectMapper(jsonObjectMapper);

        this.threadLocale = threadLocale;

        // fill list of cluster servers
        for (String uri : uris) {
            server = new Server(this, uri, threadLocale);
            try {
                server.connect();
            }
            catch (Exception e) {
                logger.error("[constructor] server '{}' is not available", uri);
            }

            SERVERS.add(server);

            if (server.isMaster()) {
                masterServer = server;
            }
        }

        // remember currently active master / write server
        setWriteServer(masterServer);
    }

    /**
     * Returns the server identified by Neo4j cluster id.
     * @param id Neo4j cluster id
     * @return server identified by id
     */
    public Server getServerById(final String id) {
        for (Server server : SERVERS) {
            if (server.getId().equals(id)) {
                return server;
            }
        }

        return null;
    }

    /**
     * Returns the server identified by Neo4j cluster id.
     * @param uri Neo4j server uri
     * @return server identified by id
     */
    public Server getServerByUri(final String uri) {
        for (Server server : SERVERS) {
            if (server.getManagementUri().equals(uri)) {
                return server;
            }
        }

        return null;
    }

    /**
     * Adds a server to the list of available servers.
     * @param id Neo4j cluster id
     */
    public synchronized void onServerAvailable(final String id, final String role) {
        logger.debug("[onServerAvailable] id = {}, role = {}", id, role);

        Server server = getServerById(id);
        boolean isMaster = role.equals("master");
        boolean isSlave = role.equals("slave");

        if (server == null) {
            return;
        }

        if (server.isAvailable()) {
            if (server.isMaster() && isMaster) return;
            if (!server.isMaster() && isSlave) return;
        }

        if (isMaster || isSlave) {
            server.setAvailable(true);
            try {
                server.connect();
            }
            catch (Exception e) {
            }
        }

        server.setAvailable(true);

        if (isMaster) {
            setWriteServer(server);
        }

        refreshServers();
    }

    /**
     * Removes a server from the list of available servers.
     * @param id Neo4j cluster id
     */
    public synchronized void onServerUnavailable(final String id) {
        logger.debug("[onServerUnavailable] id = {}", id);

        Server server = getServerById(id);

        if (server != null) {
            server.setAvailable(false);
            refreshServers();
        }
    }

    /**
     * Add a server to the list of available servers.
     * @param id Neo4j cluster id
     * @param uri websocket uri
     */
    public synchronized void onServerReconnected(final String id, final String uri) {
        logger.debug("[onServerReconnected] id = {}, uri = {}", id, uri);

        if (id.length() == 0) {
            Server server = getServerByUri(uri);
            server.register();
            server.setAvailable(true);
        }

        refreshServers();
    }

    /**
     * Changes the current master / write server.
     * @param writeServer Neo4j server to use for write access
     */
    public synchronized void setWriteServer(final Server writeServer) {
        logger.debug("[setWriteServer] uri = {}", (writeServer == null) ? "NULL" : writeServer.getDataUri());

        if (this.writeServer != null && writeServer != null && this.writeServer.equals(writeServer)) return;

        this.writeServer = writeServer;
        refreshServers();
    }

    /**
     * Reorganises the list of read servers based on current master / write server and list of available servers.
     */
    protected synchronized void refreshServers() {
        StringBuilder refreshResult = new StringBuilder();
        Set<Server> availableReadServers = new HashSet<>();
        Server[] newReadServers;

        if (writeServer == null || !writeServer.isAvailable()) {
            logger.debug("[refreshServers] write server is null or not available");

            for (Server server : SERVERS) {
                if (server.isAvailable() && server.isMaster()) {
                    writeServer = server;
                    logger.debug("[refreshServers] new writeServer: id = {}, uri = {}", writeServer.getId(), writeServer.getManagementUri());
                }
            }

            if (writeServer == null) {
                newReadServers = new Server[0];
                return;
            }
        }

        for (Server server : SERVERS) {
            logger.debug("[refreshServers] server: id = {}, isAvailable = {}", server.getId(), server.isAvailable());

            if (!server.getId().equals(writeServer.getId()) && server.isAvailable()) {
                logger.debug("[refreshServers] add read server");
                availableReadServers.add(server);
            }
        }

        if (availableReadServers.size() == 0) {
            logger.debug("[refreshServers] available read servers = 0");

            newReadServers = new Server[1];
            newReadServers[0] = writeServer;

            refreshResult
                    .append("refreshServers: read servers = ")
                    .append(writeServer.getId())
                    .append("=>")
                    .append(writeServer.getManagementUri())
                    .append(", write servers = ")
                    .append(writeServer.getId())
                    .append("=>")
                    .append(writeServer.getManagementUri());
        }
        else {
            logger.debug("[refreshServers] available read servers = {}", availableReadServers.size());

            newReadServers = new Server[availableReadServers.size()];
            int i = 0;

            refreshResult.append("refreshServers: read servers = [");

            for (Server server : availableReadServers) {
                newReadServers[i++] = server;

                refreshResult
                        .append(server.getId())
                        .append("=>")
                        .append(server.getManagementUri())
                        .append(" ");
            }

            refreshResult
                    .append(", write servers = ")
                    .append(writeServer.getId())
                    .append("=>")
                    .append(writeServer.getManagementUri())
                    .append("]");
        }

        readServers = newReadServers;

        logger.debug("[refreshServers] {}", refreshResult.toString());
    }

    /**
     * Gets an read server from the list of available servers using round robing load balancing.
     * @return server for read access
     */
    protected Server getReadServer() {
        Server[] servers = readServers;

        return servers[readSequence.incrementAndGet(servers.length)];
    }

    /**
     * Gets the currently active master / write server.
     * @return server for write access
     */
    protected Server getWriteServer() {
        return writeServer;
    }

    /**
     * Sends a message (which will probably create a write access) to the Neo4j cluster.
     * @param message binary json message (usually json format)
     * @throws Exception exception
     */
    public void sendWriteMessage(final byte[] message) throws ConnectionNotAvailableException {
        sendWriteMessage(message, getWriteServer());
    }

    public void sendWriteMessage(final byte[] message, final Server server) throws ConnectionNotAvailableException {
        DataConnection connection = server.getConnection();

        if (connection == null) {
            throw new ConnectionNotAvailableException(server);
        }

        connection.send(message);

        server.returnConnection(connection);
    }

    /**
     * Sends a message (which will probably create a write access) to the Neo4j cluster and waits for a reply.
     * @param message binary json message (usually json format)
     * @return result text message (usually json format)
     * @throws Exception exception
     */
    public byte[] sendWriteMessageWithResult(byte[] message) throws ConnectionNotAvailableException {
        return sendWriteMessageWithResult(message, getWriteServer());
    }

    public byte[] sendWriteMessageWithResult(final byte[] message, final Server server) throws ConnectionNotAvailableException {
        DataConnection connection = server.getConnection();

        if (connection == null) {
            throw new ConnectionNotAvailableException(server);
        }

        byte[] result = connection.sendWithResult(message);

        server.returnConnection(connection);

        return result;
    }

    /**
     * Sends a message (only read access) to the Neo4j cluster and waits for a reply.
     * @param message binary json message
     * @return result binary json message
     * @throws Exception exception
     */
    public byte[] sendReadMessage(final byte[] message) throws ConnectionNotAvailableException {
        return sendReadMessage(message, getReadServer());
    }

    public byte[] sendReadMessage(final byte[] message, final Server server) throws ConnectionNotAvailableException{
        DataConnection connection = server.getConnection();

        if (connection == null) {
            throw new ConnectionNotAvailableException(server);
        }

        byte[] result = connection.sendWithResult(message);

        server.returnConnection(connection);

        return result;
    }
}
