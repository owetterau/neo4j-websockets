package de.oliverwetterau.neo4j.websockets.server.web;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.oliverwetterau.neo4j.websockets.server.ha.HighAvailabilityConfiguration;
import de.oliverwetterau.neo4j.websockets.core.data.ManagementCommand;
import de.oliverwetterau.neo4j.websockets.core.data.json.JsonObjectMapper;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Created by oliver on 13.11.15.
 */
@Service
public class ManagementHandler {
    private static final Logger logger = LoggerFactory.getLogger(ManagementHandler.class);

    private final HighAvailabilityConfiguration highAvailabilityConfiguration;
    private final JsonObjectMapper jsonObjectMapper;

    @Autowired
    public ManagementHandler(final JsonObjectMapper jsonObjectMapper) {
        this.highAvailabilityConfiguration = HighAvailabilityConfiguration.instance();
        this.jsonObjectMapper = jsonObjectMapper;
    }

    public void handleMessage(final WebSocketChannel channel, final String message) {
        logger.debug("[handleMessage] session = {}, message = {}", channel.toString(), message);

        if (message.equals(ManagementCommand.REGISTER)) {
            ObjectNode objectNode = jsonObjectMapper.getObjectMapper().createObjectNode();
            objectNode.put("id", highAvailabilityConfiguration.getId());
            objectNode.put("isMaster", highAvailabilityConfiguration.isMaster());

            sendMessage(channel, objectNode);

            logger.debug("[handleMessage] REGISTER: {}", objectNode.toString());
        }
    }

    public void sendMessage(final WebSocketChannel channel, final ObjectNode message) {
        try {
            WebSockets.sendText(jsonObjectMapper.getObjectMapperText().writeValueAsString(message), channel, null);
        }
        catch (IOException e) {
            logger.error("[sendMessage]", e);
        }
    }
}
