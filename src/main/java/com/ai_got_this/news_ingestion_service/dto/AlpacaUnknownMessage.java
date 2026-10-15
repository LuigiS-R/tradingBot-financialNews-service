package com.ai_got_this.news_ingestion_service.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Data
public class AlpacaUnknownMessage extends AlpacaMessage {
    private Map<String, Object> propertiesMap = new HashMap<>();

    @JsonAnySetter
    public void put(String key, Object value) {
        propertiesMap.put(key, value);
    }

    @Override
    public String toString() {
        return mapToJson(propertiesMap);
    }

    private String mapToJson(Map<?, ?> map) {
        StringBuilder builder = new StringBuilder("{");

        for(Map.Entry<?, ?> entry : map.entrySet()) {
            builder.append(entry.getKey())
                    .append(": ")
                    .append(valueToJson(entry.getValue()))
                    .append(", ");
        }
        return builder.append("}").toString();
    }

    private String valueToJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Map<?, ?> nestedMap) {
            return mapToJson(nestedMap);
        }
        if (value instanceof Collection<?> collection) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;
            for (Object item : collection) {
                if (!first) {
                    builder.append(", ");
                }
                builder.append(valueToJson(item));
                first = false;
            }
            return builder.append("]").toString();
        }
        return String.valueOf(value);
    }
}