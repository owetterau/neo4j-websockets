package de.oliverwetterau.neo4j.websockets.server;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.oliverwetterau.neo4j.websockets.core.data.ManagementCommand;
import de.oliverwetterau.neo4j.websockets.core.data.json.JsonObjectMapper;
import de.oliverwetterau.neo4j.websockets.server.neo4j.EmbeddedNeo4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * Created by oliver on 25.08.15.
 */
@Service
public class ManagementHandler {
    private static final Logger logger = LoggerFactory.getLogger(ManagementHandler.class);

    protected final JsonObjectMapper jsonObjectMapper;
    protected final EmbeddedNeo4j embeddedNeo4j;

    @Autowired
    public ManagementHandler(final EmbeddedNeo4j embeddedNeo4j, final JsonObjectMapper jsonObjectMapper) {
        this.embeddedNeo4j = embeddedNeo4j;
        this.jsonObjectMapper = jsonObjectMapper;
    }

    public void handleMessage(final WebSocketSession session, final String message) {
        logger.debug("[handleMessage] session = {}, message = {}", session.getId(), message);

        if (message.equals(ManagementCommand.REGISTER)) {
            ObjectNode objectNode = jsonObjectMapper.getObjectMapper().createObjectNode();
            objectNode.put("id", embeddedNeo4j.getHighAvailabilityId());
            objectNode.put("isMaster", embeddedNeo4j.isMaster());

            sendMessage(session, objectNode);

            logger.debug("[handleMessage] REGISTER: {}", objectNode.toString());
        }
    }

    protected void sendMessage(final WebSocketSession session, final ObjectNode message) {
        try {
            session.sendMessage(new BinaryMessage(jsonObjectMapper.getObjectMapper().writeValueAsBytes(message)));
        }
        catch (IOException e) {
            logger.error("[sendMessage]", e);
        }
    }
}
