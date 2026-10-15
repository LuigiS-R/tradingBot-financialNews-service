package com.ai_got_this.news_ingestion_service.dto;

import lombok.Data;

@Data
public class AlpacaErrorMessage extends AlpacaMessage{
    private String msg;
    private Integer code;
}
