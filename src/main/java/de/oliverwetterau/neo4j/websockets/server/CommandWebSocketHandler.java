package de.oliverwetterau.neo4j.websockets.server;

import com.fasterxml.jackson.databind.JsonNode;
import de.oliverwetterau.neo4j.websockets.core.data.json.JsonObjectMapper;
import de.oliverwetterau.neo4j.websockets.core.helpers.ExceptionConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

/**
 * Created by oliver on 25.08.15.
 */
@Service
public class CommandWebSocketHandler extends BinaryWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(CommandWebSocketHandler.class);

    protected final JsonObjectMapper jsonObjectMapper;
    protected final CommandHandler commandHandler;

    @Autowired
    public CommandWebSocketHandler(final CommandHandler commandHandler, final JsonObjectMapper jsonObjectMapper) {
        this.commandHandler = commandHandler;
        this.jsonObjectMapper = jsonObjectMapper;
    }

    @Override
    public void handleBinaryMessage(final WebSocketSession session, final BinaryMessage message) {
        try {
            byte[] messageArray = new byte[message.getPayloadLength()];
            message.getPayload().get(messageArray);
            JsonNode jsonNode = jsonObjectMapper.getObjectMapper().readTree(messageArray);

            commandHandler.handleMessage(session, jsonNode);
        }
        catch (Exception e) {
            logger.error("[handleBinaryMessage] {} \n => {}", e, ExceptionConverter.stackTrace(e));
        }
    }
}
