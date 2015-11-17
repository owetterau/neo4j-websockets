package de.oliverwetterau.neo4j.websockets.server.json;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import de.oliverwetterau.neo4j.websockets.core.data.json.JsonObjectSerializers;

import java.util.HashMap;
import java.util.Map;

public class DefaultJsonObjectSerializers implements JsonObjectSerializers {
    @Override
    public Map<Class, JsonSerializer> getSerializers() {
        return new HashMap<>();
    }

    @Override
    public Map<Class, JsonDeserializer> getDeserializers() {
        return new HashMap<>();
    }
}
