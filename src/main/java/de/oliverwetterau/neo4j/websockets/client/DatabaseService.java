package de.oliverwetterau.neo4j.websockets.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.oliverwetterau.neo4j.websockets.core.data.CommandParameters;
import de.oliverwetterau.neo4j.websockets.core.data.Error;
import de.oliverwetterau.neo4j.websockets.core.data.Result;
import de.oliverwetterau.neo4j.websockets.core.data.json.JsonObjectMapper;
import de.oliverwetterau.neo4j.websockets.core.helpers.ExceptionConverter;
import de.oliverwetterau.neo4j.websockets.core.i18n.ThreadLocale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Provides methods to send read and write messages to a Neo4j cluster.
 *
 * @author Oliver Wetterau
 * @version 2015-09-01
 */
@Service
public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    /** wrapper for json object mapper */
    protected final JsonObjectMapper jsonObjectMapper;
    /** manager for database connections */
    protected final Database database;
    /** language settings */
    protected final ThreadLocale threadLocale;

    /**
     * Constructor
     * @param database database connections manager
     * @param jsonObjectMapper wrapper for json object mapper
     * @param threadLocale language settings
     */
    @Autowired
    public DatabaseService(Database database, JsonObjectMapper jsonObjectMapper, ThreadLocale threadLocale) {
        this.database = database;
        this.jsonObjectMapper = jsonObjectMapper;
        this.threadLocale = threadLocale;
    }

    /**
     * Sends a read message to a Neo4j cluster and returns the data server's answer.
     * @param service the service of the data server to be used
     * @param method the method of the service to be used
     * @return data server's answer
     */
    public Result<JsonNode> getData(final String service, final String method) {
        return getData(service, method, threadLocale.getLocale());
    }

    /**
     * Sends a read message to a Neo4j cluster and returns the data server's answer.
     * @param service the service of the data server to be used
     * @param method the method of the service to be used
     * @param json a json node containing parameters for the method
     * @return data server's answer
     */
    public Result<JsonNode> getData(final String service, final String method, final JsonNode json) {
        return getData(service, method, threadLocale.getLocale(), json);
    }

    /**
     * Sends a read message to a Neo4j cluster and returns the data server's answer.
     * @param service the service of the data server to be used
     * @param method the method of the service to be used
     * @param locale the language settings to be used by the method
     * @return data server's answer
     */
    public Result<JsonNode> getData(final String service, final String method, final Locale locale) {
        ObjectMapper objectMapper = jsonObjectMapper.getObjectMapper();

        ObjectNode objectNode = objectMapper.createObjectNode();

        objectNode.put(CommandParameters.SERVICE, service);
        objectNode.put(CommandParameters.METHOD, method);
        objectNode.put(CommandParameters.LANGUAGE, locale.getLanguage());

        return getData(objectNode, objectMapper);
    }

    /**
     * Sends a read message to a Neo4j cluster and returns the data server's answer.
     * @param service the service of the data server to be used
     * @param method the method of the service to be used
     * @param locale the language settings to be used by the method
     * @param json a json node containing parameters for the method
     * @return data server's answer
     */
    public Result<JsonNode> getData(final String service, final String method, final Locale locale, final JsonNode json) {
        ObjectMapper objectMapper = jsonObjectMapper.getObjectMapper();

        ObjectNode objectNode = objectMapper.createObjectNode();

        objectNode.put(CommandParameters.SERVICE, service);
        objectNode.put(CommandParameters.METHOD, method);
        objectNode.put(CommandParameters.LANGUAGE, locale.getLanguage());
        objectNode.set(CommandParameters.PARAMETERS, json);

        return getData(objectNode, objectMapper);
    }

    /**
     * Sends a read message to a Neo4j cluster and returns the data server's answer.
     * @param message service name, method name, language settingsa and method parameters in one json node
     * @param objectMapper json object mapper used for serialization
     * @return data server's answer
     */
    @SuppressWarnings("unchecked")
    protected Result<JsonNode> getData(final ObjectNode message, final ObjectMapper objectMapper) {
        Result<JsonNode> result;
        byte[] resultMessage;

        // convert json into map
        try {
            resultMessage = database.sendReadMessage(objectMapper.writeValueAsBytes(message));
        }
        catch (Exception e) {
            logger.error("[getData(ObjectNode)] could not read from database", e);
            return new Result<>(new Error(Error.NO_DATABASE_REPLY, ExceptionConverter.toString(e)));
        }

        try {
            result = jsonObjectMapper.getObjectMapper().readValue(resultMessage, Result.class);
        }
        catch (Exception e) {
            logger.error("[writeDataWithResult] could not convert message to json: '{}'", resultMessage, e);
            return new Result<>(new Error(Error.MESSAGE_TO_JSON_FAILURE, ExceptionConverter.toString(e)));
        }

        return result;
    }

    /**
     * Sends a write message to a Neo4j cluster and returns the data server's answer.
     * @param service the service of the data server to be used
     * @param method the method of the service to be used
     * @return data server's answer
     */
    public Result<JsonNode> writeDataWithResult(final String service, final String method) {
        return writeDataWithResult(service, method, threadLocale.getLocale());
    }

    /**
     * Sends a write message to a Neo4j cluster and returns the data server's answer.
     * @param service the service of the data server to be used
     * @param method the method of the service to be used
     * @param locale the language settings to be used by the method
     * @return data server's answer
     */
    public Result<JsonNode> writeDataWithResult(final String service, final String method, final Locale locale) {
        ObjectMapper objectMapper = jsonObjectMapper.getObjectMapper();

        ObjectNode objectNode = objectMapper.createObjectNode();

        objectNode.put(CommandParameters.SERVICE, service);
        objectNode.put(CommandParameters.METHOD, method);
        objectNode.put(CommandParameters.LANGUAGE, locale.getLanguage());

        return writeDataWithResult(objectNode, objectMapper);
    }

    /**
     * Sends a write message to a Neo4j cluster and returns the data server's answer.
     * @param service the service of the data server to be used
     * @param method the method of the service to be used
     * @param json a json node containing parameters for the method
     * @return data server's answer
     */
    public Result<JsonNode> writeDataWithResult(final String service, final String method, final JsonNode json) {
        return writeDataWithResult(service, method, threadLocale.getLocale(), json);
    }

    /**
     * Sends a write message to a Neo4j cluster and returns the data server's answer.
     * @param service the service of the data server to be used
     * @param method the method of the service to be used
     * @param locale the language settings to be used by the method
     * @param json a json node containing parameters for the method
     * @return data server's answer
     */
    public Result<JsonNode> writeDataWithResult(final String service, final String method, final Locale locale, final JsonNode json) {
        ObjectMapper objectMapper = jsonObjectMapper.getObjectMapper();

        ObjectNode objectNode = objectMapper.createObjectNode();

        objectNode.put(CommandParameters.SERVICE, service);
        objectNode.put(CommandParameters.METHOD, method);
        objectNode.put(CommandParameters.LANGUAGE, locale.getLanguage());
        objectNode.set(CommandParameters.PARAMETERS, json);

        return writeDataWithResult(objectNode, objectMapper);
    }

    /**
     * Sends a write message to a Neo4j cluster and returns the data server's answer.
     * @param message service name, method name, language settingsa and method parameters in one json node
     * @param objectMapper json object mapper used for serialization
     * @return data server's answer
     */
    @SuppressWarnings("unchecked")
    protected Result<JsonNode> writeDataWithResult(final ObjectNode message, final ObjectMapper objectMapper) {
        Result<JsonNode> result;
        byte[] resultMessage;

        // convert json into map
        try {
            resultMessage = database.sendWriteMessageWithResult(objectMapper.writeValueAsBytes(message));
        }
        catch (Exception e) {
            logger.error("[writeDataWithResult] could not read from database", e);
            return new Result<>(new Error(Error.NO_DATABASE_REPLY, ExceptionConverter.toString(e)));
        }

        try {
            result = jsonObjectMapper.getObjectMapper().readValue(resultMessage, Result.class);
        }
        catch (Exception e) {
            logger.error("[writeDataWithResult] could not convert message to json: '{}'", resultMessage, e);
            return new Result<>(new Error(Error.MESSAGE_TO_JSON_FAILURE, ExceptionConverter.toString(e)));
        }

        return result;
    }
}

