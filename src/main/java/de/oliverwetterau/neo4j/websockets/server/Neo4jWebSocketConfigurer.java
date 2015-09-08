package de.oliverwetterau.neo4j.websockets.server;

import de.oliverwetterau.neo4j.websockets.core.helpers.WebsocketSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * This class activates websockets and defines the communication channels to be used for management and data connections.
 */
@Configuration
@EnableWebSocket
public class Neo4jWebSocketConfigurer implements WebSocketConfigurer {
    protected CommandWebSocketHandler commandWebSocketHandler;
    protected ManagementWebSocketHandler managementWebSocketHandler;

    public void registerWebSocketHandlers(final WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(managementWebSocketHandler, "/" + WebsocketSettings.MANAGEMENT_CONNECTION).setAllowedOrigins("*");
        webSocketHandlerRegistry.addHandler(commandWebSocketHandler, "/" + WebsocketSettings.COMMAND_CONNECTION).setAllowedOrigins("*");
    }

    @Autowired
    public void setCommandWebSocketHandler(final CommandWebSocketHandler commandWebSocketHandler) {
        this.commandWebSocketHandler = commandWebSocketHandler;
    }

    @Autowired
    public void setManagementWebSocketHandler(final ManagementWebSocketHandler managementWebSocketHandler) {
        this.managementWebSocketHandler = managementWebSocketHandler;
    }
}
