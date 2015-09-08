package de.oliverwetterau.neo4j.websockets.core.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import de.oliverwetterau.neo4j.websockets.core.data.json.JsonObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class is used as a generic Result for data requests. It allows for automatic conversion of BaseEntity derived
 * classes to Json.
 *
 * @author Oliver Wetterau
 * @version 2015-09-01
 */
public class Result<T> {
    private static final Logger logger = LoggerFactory.getLogger(Result.class);
    protected static JsonObjectMapper jsonObjectMapper;

    /** list of all errors that appeared while trying to retrieve data */
    protected List<Error> errors;
    /** list of all data elements that were retrieved */
    protected List<T> data = new ArrayList<>();

    /** if "jsonString" does not represent the current state of this result, isStringDirty is true */
    protected boolean isStringDirty = true;
    /** current jsonString representation of this result */
    protected String jsonString = null;

    /** if "jsonBytes" does not represent the current state of this result, isBytesDirty is true */
    protected boolean isBytesDirty = true;
    /** current json representation of this result */
    protected byte[] jsonBytes = null;

    /**
     * Constructor
     */
    public Result() {
    }

    public Result(Class<T> clazz) {
    }

    public Result(int size) {
        data = new ArrayList<>(size);
    }

    public Result(final T object) {
        data.add(object);
    }

    public Result(final Error error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }

        errors.add(error);
    }

    public Result(final List<Error> errors) {
        this.errors = new ArrayList<>();
        this.errors.addAll(errors);
    }

    /**
     * Set the object mapper factory
     * @param jsonObjectMapper object mapper factory
     */
    public static void setJsonObjectMapper(final JsonObjectMapper jsonObjectMapper) {
        Result.jsonObjectMapper = jsonObjectMapper;
    }

    public void add(final Error error) {
        isStringDirty = true;
        isBytesDirty = true;
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
    }

    public void addErrors(final Collection<Error> errors) {
        isStringDirty = true;
        isBytesDirty = true;
        this.errors.addAll(errors);
    }

    public List<Error> getErrors() {
        return errors;
    }

    public String errorsToHtml() {
        StringBuilder stringBuilder = new StringBuilder();

        for (Error error : errors) {
            stringBuilder.append(error.toHtml());
        }

        return stringBuilder.toString();
    }

    public void add(final T object) {
        isStringDirty = true;
        isBytesDirty = true;
        data.add(object);
    }

    public void add(final Collection<T> objects) {
        isStringDirty = true;
        isBytesDirty = true;
        errors.forEach(this::add);
    }

    public List<T> getData() {
        return data;
    }

    public T getSingleData() {
        return data.get(0);
    }

    public boolean isOk() {
        return errors == null || errors.size() == 0;
    }

    public String toJsonString() {
        if (!isStringDirty) {
            return jsonString;
        }

        try {
            jsonString = jsonObjectMapper.getObjectMapper().writeValueAsString(this);
            isStringDirty = false;
        }
        catch (JsonProcessingException e) {
            logger.error("[toJsonString]", e);
            return null;
        }

        return jsonString;
    }

    @JsonIgnore
    public byte[] toJsonBytes() {
        if (!isBytesDirty) {
            return jsonBytes;
        }

        try {
            jsonBytes = jsonObjectMapper.getObjectMapper().writeValueAsBytes(this);
            isBytesDirty = false;
        }
        catch (JsonProcessingException e) {
            logger.error("[toJsonBytes]", e);
            return null;
        }

        return jsonBytes;
    }
}
