package de.oliverwetterau.neo4j.websockets.core.data.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import de.oliverwetterau.neo4j.websockets.core.data.Error;
import de.oliverwetterau.neo4j.websockets.core.data.Result;

import java.io.IOException;

/**
 * Created by oliver on 01.09.15.
 */
public class ResultDeserializer extends JsonDeserializer<Result> {
    @Override
    public Result deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException, JsonProcessingException
    {
        JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
        Result<JsonNode> result = new Result<>();

        if (jsonNode.has("Errors")) {
            for (JsonNode node : jsonNode.get("Errors")) {
                result.add(new Error(node));
            }
        }

        if (jsonNode.has("Data")) {
            for (JsonNode node : jsonNode.get("Data")) {
                result.add(node);
            }
        }

        return result;
    }
}
