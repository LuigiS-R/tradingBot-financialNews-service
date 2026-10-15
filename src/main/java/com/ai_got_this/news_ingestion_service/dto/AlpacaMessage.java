package com.ai_got_this.news_ingestion_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.util.List;

@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "T",
        visible = true,
        defaultImpl = AlpacaUnknownMessage.class
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AlpacaNewsMessage.class, name = "n"),
        @JsonSubTypes.Type(value = AlpacaErrorMessage.class, name = "error"),
        @JsonSubTypes.Type(value = AlpacaSuccessMessage.class, name = "success"),
        @JsonSubTypes.Type(value = AlpacaErrorMessage.class, name = "other")
})
public abstract class AlpacaMessage {
    @JsonProperty("T")
    private String type;
}
