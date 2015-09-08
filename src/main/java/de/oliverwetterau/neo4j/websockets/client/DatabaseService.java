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
 * Created by oliver on 02.01.15.
 */
@Service
public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    protected final JsonObjectMapper jsonObjectMapper;
    protected final Database database;
    protected final ThreadLocale threadLocale;

    @Autowired
    public DatabaseService(Database database, JsonObjectMapper jsonObjectMapper, ThreadLocale threadLocale) {
        this.database = database;
        this.jsonObjectMapper = jsonObjectMapper;
        this.threadLocale = threadLocale;
    }

    public Result<JsonNode> getData(final String topic, final String command) {
        return getData(topic, command, threadLocale.getLocale());
    }

    public Result<JsonNode> getData(final String topic, final String command, final JsonNode json) {
        return getData(topic, command, threadLocale.getLocale(), json);
    }

    public Result<JsonNode> getData(final String service, final String method, final Locale locale) {
        ObjectMapper objectMapper = jsonObjectMapper.getObjectMapper();

        ObjectNode objectNode = objectMapper.createObjectNode();

        objectNode.put(CommandParameters.SERVICE, service);
        objectNode.put(CommandParameters.METHOD, method);
        objectNode.put(CommandParameters.LANGUAGE, locale.getLanguage());

        return getData(objectNode, objectMapper);
    }

    public Result<JsonNode> getData(final String service, final String method, final Locale locale, final JsonNode json) {
        ObjectMapper objectMapper = jsonObjectMapper.getObjectMapper();

        ObjectNode objectNode = objectMapper.createObjectNode();

        objectNode.put(CommandParameters.SERVICE, service);
        objectNode.put(CommandParameters.METHOD, method);
        objectNode.put(CommandParameters.LANGUAGE, locale.getLanguage());
        objectNode.set(CommandParameters.PARAMETERS, json);

        return getData(objectNode, objectMapper);
    }

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

    public Result<JsonNode> writeDataWithResult(final String service, final String method) {
        return writeDataWithResult(service, method, threadLocale.getLocale());
    }

    public Result<JsonNode> writeDataWithResult(final String service, final String method, final Locale locale) {
        ObjectMapper objectMapper = jsonObjectMapper.getObjectMapper();

        ObjectNode objectNode = objectMapper.createObjectNode();

        objectNode.put(CommandParameters.SERVICE, service);
        objectNode.put(CommandParameters.METHOD, method);

        return writeDataWithResult(objectNode, objectMapper);
    }

    public Result<JsonNode> writeDataWithResult(final String service, final String method, final JsonNode json) {
        return writeDataWithResult(service, method, json, threadLocale.getLocale());
    }

    public Result<JsonNode> writeDataWithResult(final String service, final String method, final JsonNode json, final Locale locale) {
        ObjectMapper objectMapper = jsonObjectMapper.getObjectMapper();

        ObjectNode objectNode = objectMapper.createObjectNode();

        objectNode.put(CommandParameters.SERVICE, service);
        objectNode.put(CommandParameters.METHOD, method);
        objectNode.set(CommandParameters.PARAMETERS, json);

        return writeDataWithResult(objectNode, objectMapper);
    }

    protected Result<JsonNode> writeDataWithResult(final ObjectNode objectNode, final ObjectMapper objectMapper) {
        Result<JsonNode> result;
        byte[] resultMessage;

        // convert json into map
        try {
            resultMessage = database.sendWriteMessageWithResult(objectMapper.writeValueAsBytes(objectNode));
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

