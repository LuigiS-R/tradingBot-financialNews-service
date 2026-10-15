package com.ai_got_this.news_ingestion_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class AlpacaNewsMessage extends AlpacaMessage {
    private String id;
    private String headline;
    private List<String> symbols;
}
