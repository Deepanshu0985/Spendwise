package com.finance.application.assistant.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small helpers for writing the JSON-schema argument descriptions the model is shown. */
final class ToolSchemas {

    private ToolSchemas() {
    }

    static Map<String, Object> object(Map<String, Object> properties, String... required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of(required));
        schema.put("additionalProperties", false);
        return schema;
    }

    static Map<String, Object> date(String description) {
        return Map.of("type", "string", "description", description + " Written yyyy-mm-dd.");
    }

    static Map<String, Object> text(String description) {
        return Map.of("type", "string", "description", description);
    }

    static Map<String, Object> bool(String description) {
        return Map.of("type", "boolean", "description", description);
    }

    static Map<String, Object> integer(String description) {
        return Map.of("type", "integer", "description", description);
    }

    /** Properties in the order given. */
    static Map<String, Object> props(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
