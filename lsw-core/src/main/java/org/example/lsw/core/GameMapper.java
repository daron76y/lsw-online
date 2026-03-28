package org.example.lsw.core;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Jackson ObjectMapper used by all of the other services for serialization/deserialization of
 * game data (unit, party, etc.) to/from JSON for database storage and communication via HTTP.
 * Having a single ObjectMapper used across the project ensures everything serializes/deserializes
 * the exact same, so now mismatch issues can occur.
 */
public class GameMapper {
    //example of lazy-init singleton
    //this ObjectMapper allows us to force the JVM to access class members regardless of
    //access modifiers or final constants/ It ignores getters/setters because some of them
    //either contain logical side effects or are calculated/derived values
    private static final ObjectMapper INSTANCE = JsonMapper.builder()
            .enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
            .enable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)
            .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .visibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
            .visibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
            .visibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
            .build();

    private GameMapper() {}

    public static ObjectMapper get() { return INSTANCE; }

    public static String toJson(Object obj) {
        try {return INSTANCE.writeValueAsString(obj);}
        catch (Exception e) {throw new RuntimeException("Serialization failed", e);}
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try { return INSTANCE.readValue(json, type); }
        catch (Exception e) { throw new RuntimeException("Deserialization failed for " + type.getSimpleName(), e); }
    }
}
