package de.oliverwetterau.neo4j.websockets.core.data.json;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import de.oliverwetterau.neo4j.websockets.core.data.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Wrapper class for a json object mapper.
 *
 * @author Oliver Wetterau
 * @version 2015-09-01
 */
@Service
public class JsonObjectMapper {
    protected static final ObjectMapper objectMapper = new ObjectMapper(new SmileFactory());

    /**
     * Constructor
     * Adds the provided list of custom serializers to the object mapper returned by this class.
     * @param jsonObjectSerializers list of custom serializers to be used by the object mapper
     */
    @Autowired
    public JsonObjectMapper(JsonObjectSerializers jsonObjectSerializers) {
        SimpleModule entityModule = new SimpleModule("de.oliverwetterau.neo4j.websockets");

        entityModule.addSerializer(Result.class, new ResultSerializer());
        entityModule.addSerializer(de.oliverwetterau.neo4j.websockets.core.data.Error.class, new ErrorSerializer());

        entityModule.addDeserializer(Result.class, new ResultDeserializer());

        if (jsonObjectSerializers != null) {
            for (Map.Entry<Class,JsonSerializer> jsonSerializer : jsonObjectSerializers.getSerializers().entrySet()) {
                entityModule.addSerializer(jsonSerializer.getKey(), jsonSerializer.getValue());
            }

            for (Map.Entry<Class,JsonDeserializer> jsonDeserializer : jsonObjectSerializers.getDeserializers().entrySet()) {
                entityModule.addDeserializer(jsonDeserializer.getKey(), jsonDeserializer.getValue());
            }
        }

        objectMapper.registerModule(entityModule);
    }

    /**
     * Returns an object mapper that can be used for serialization and deserialization.
     * @return object mapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
