package de.oliverwetterau.neo4j.websockets.server.neo4j;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.oliverwetterau.neo4j.websockets.core.data.Error;
import de.oliverwetterau.neo4j.websockets.core.data.json.JsonObjectMapper;
import org.neo4j.graphdb.ConstraintViolationException;
import org.neo4j.graphdb.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by oliver on 10.11.15.
 */
@Service
public class ExceptionToErrorConverter {
    protected final JsonObjectMapper jsonObjectMapper;

    @Autowired
    public ExceptionToErrorConverter(final JsonObjectMapper jsonObjectMapper) {
        this.jsonObjectMapper = jsonObjectMapper;
    }

    public Error convert(final Exception exception) {
        Class errorClass = exception.getClass();
        Class causeClass;

        if (errorClass.equals(ConstraintViolationException.class)) {
            causeClass = exception.getCause().getClass();

            /*if (causeClass.equals(UniqueConstraintViolationKernelException.class)) {
                return analyseUniqueConstraintViolation((ConstraintViolationException) exception);
            }*/
        }
        else if (errorClass.equals(NotFoundException.class)) {
            return analyseNotFound((NotFoundException) exception);
        }

        return new Error(
                Error.EXCEPTION,
                exception.getMessage()
        );
    }

    protected Error analyseUniqueConstraintViolation(final ConstraintViolationException constraintViolationException) {
        String errorMessage = constraintViolationException.getMessage();

        final String part1 = "^Node ";
        final String part2 = " already exists with label ";
        final String part3 = " and property ";
        final String part4 = "\"=[";
        final String part5 = "]";

        errorMessage = errorMessage.replaceFirst(part1, "");
        int i = errorMessage.indexOf(part2);
        Long nodeId = Long.parseLong(errorMessage.substring(0, i));

        errorMessage = errorMessage.substring(i + part2.length());
        i = errorMessage.indexOf(part3);
        String label = errorMessage.substring(0, i);

        errorMessage = errorMessage.substring(i + part3.length());
        i = errorMessage.indexOf(part4);
        String propertyName = errorMessage.substring(1, i);

        String propertyValue = errorMessage.substring(i + part4.length(), errorMessage.lastIndexOf(part5));

        ObjectNode detailsNode = jsonObjectMapper.getObjectMapper().createObjectNode();

        detailsNode.put("NodeId", nodeId);
        detailsNode.put("Label", label);
        detailsNode.put("Property", propertyName);
        detailsNode.put("Value", propertyValue);

        return new Error(
                Error.UNIQUE_CONSTRAINT_VIOLATION,
                constraintViolationException.getMessage(),
                detailsNode
        );
    }

    protected Error analyseNotFound(final NotFoundException notFoundException) {
        ObjectNode detailsNode = jsonObjectMapper.getObjectMapper().createObjectNode();

        return new Error(
                Error.NOT_FOUND,
                notFoundException.getMessage(),
                detailsNode
        );
    }
}
