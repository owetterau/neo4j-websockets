package de.oliverwetterau.neo4j.websockets.core.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.oliverwetterau.neo4j.websockets.core.data.json.JsonObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class describes errors that might appear (mostly on server side). It is used to store Exceptions and
 * errors (e.g. dataset not found) in a format that can easily be serialized to json format.
 *
 * @author Oliver Wetterau
 * @version 2015-09-01
 */
public class Error {
    private static final Logger logger = LoggerFactory.getLogger(Error.class);
    protected static JsonObjectMapper jsonObjectMapper;

    // error parameter names for json
    protected static final String TYPE = "type";
    protected static final String MESSAGE = "message";
    protected static final String DETAILS = "details";

    // json node holding all error data
    protected final ObjectNode objectNode;

    // default error types
    public static final String EXCEPTION = "Exception";
    public static final String NO_DATABASE_REPLY ="NoDatabaseReply";
    public static final String MESSAGE_TO_JSON_FAILURE = "MessageToJsonFailure";
    public static final String UNKNOWN_SERVICE = "UnknownService";
    public static final String SERVICE_HAS_NO_METHODS = "ServiceHasNoMethods";
    public static final String UNKNOWN_SERVICE_METHOD = "UnknownServiceMethod";
    public static final String METHOD_EXECUTION_FAILED = "MethodExecutionFailed";
    public static final String NOT_FOUND = "NotFound";
    public static final String UNIQUE_CONSTRAINT_VIOLATION = "UniqueConstraintViolation";

    /**
     * Constructor
     * @param type error type
     * @param message error description
     * @param details details of the error in json format
     */
    public Error(final String type, final String message, final JsonNode details) {
        assert (type != null && message != null);

        objectNode = jsonObjectMapper.getObjectMapper().createObjectNode();

        objectNode.put(TYPE, type);
        objectNode.put(MESSAGE, message);
        objectNode.set(DETAILS, details);
    }

    /**
     * Constructor
     * @param type error type
     * @param message error description
     */
    public Error(final String type, final String message) {
        this(type, message, null);
    }

    /**
     * Constructor
     * @param jsonNode type, message and details of the error in json format
     */
    public Error(final JsonNode jsonNode) {
        objectNode = jsonObjectMapper.getObjectMapper().createObjectNode();

        objectNode.put(TYPE, jsonNode.get(TYPE).asText());
        objectNode.put(MESSAGE, jsonNode.get(MESSAGE).asText());
        if (jsonNode.has(DETAILS)) {
            objectNode.put(DETAILS, jsonNode.get(DETAILS).asText());
        }
        else {
            objectNode.put(DETAILS, (String) null);
        }
    }

    /**
     * Set the object mapper factory
     * @param jsonObjectMapper object mapper factory
     */
    public static void setJsonObjectMapper(final JsonObjectMapper jsonObjectMapper) {
        Error.jsonObjectMapper = jsonObjectMapper;
    }

    /**
     * Get the error to a json node
     * @return error in json format
     */
    public ObjectNode toJson() {
        return objectNode;
    }

    /**
     * Get the error to a json string
     * @return error in json format
     */
    @Override
    public String toString() {
        return objectNode.toString();
    }

    /**
     * Get the error as a string in html format
     * @return error in html format
     */
    public String toHtml() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder
                .append("<p>")
                .append(objectNode.get(TYPE).asText()).append(": ")
                .append(objectNode.get(MESSAGE).asText()).append(" -> ")
                .append(objectNode.get(DETAILS).toString())
                .append("</p>");

        return stringBuilder.toString();
    }
}
