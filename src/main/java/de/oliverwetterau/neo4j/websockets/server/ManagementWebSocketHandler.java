package de.oliverwetterau.neo4j.websockets.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.oliverwetterau.neo4j.websockets.core.data.CommandParameters;
import de.oliverwetterau.neo4j.websockets.core.data.json.JsonObjectMapper;
import de.oliverwetterau.neo4j.websockets.core.helpers.ExceptionConverter;
import de.oliverwetterau.neo4j.websockets.server.neo4j.EmbeddedNeo4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by oliver on 25.08.15.
 */
@Service
public class ManagementWebSocketHandler extends BinaryWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(ManagementWebSocketHandler.class);

    protected final JsonObjectMapper jsonObjectMapper;
    protected final Set<WebSocketSession> webSocketSessions = new HashSet<>();

    protected final EmbeddedNeo4j embeddedNeo4j;
    protected final ManagementHandler managementHandler;

    @Autowired
    public ManagementWebSocketHandler(final EmbeddedNeo4j embeddedNeo4j, final ManagementHandler managementHandler, final JsonObjectMapper jsonObjectMapper) {
        this.embeddedNeo4j = embeddedNeo4j;
        this.managementHandler = managementHandler;
        this.jsonObjectMapper = jsonObjectMapper;

        embeddedNeo4j.addManagementWebSocketHandlerToHighAvailabilityListener(this);
    }

    public Set<WebSocketSession> getWebSocketSessions() {
        return Collections.unmodifiableSet(webSocketSessions);
    }

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) {
        webSocketSessions.add(session);
    }

    @Override
    public void handleBinaryMessage(final WebSocketSession session, final BinaryMessage message) {
        try {
            byte[] messageArray = new byte[message.getPayloadLength()];
            message.getPayload().get(messageArray);
            JsonNode jsonNode = jsonObjectMapper.getObjectMapper().readTree(messageArray);

            managementHandler.handleMessage(session, jsonNode.get(CommandParameters.METHOD).asText());
        }
        catch (Exception e) {
            logger.error("[handleBinaryMessage] {} \n => {}", e, ExceptionConverter.stackTrace(e));
        }
    }

    @Override
    public void handleTransportError(final WebSocketSession session, final Throwable exception) {
    }

    @Override
    public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status) {
        webSocketSessions.remove(session);
    }

    public void informAvailableMember(final String memberId, final String role) {
        ObjectNode objectNode = jsonObjectMapper.getObjectMapper().createObjectNode();
        objectNode.put("available", memberId);
        objectNode.put("role", role);

        informAllClients(objectNode);
    }

    public void informUnavailableMember(final String memberId, final String role) {
        ObjectNode objectNode = jsonObjectMapper.getObjectMapper().createObjectNode();
        objectNode.put("unavailable", memberId);
        objectNode.put("role", role);

        informAllClients(objectNode);
    }

    protected void informAllClients(final ObjectNode message) {
        for (WebSocketSession session : getWebSocketSessions()) {
            managementHandler.sendMessage(session, message);
        }
    }
}
