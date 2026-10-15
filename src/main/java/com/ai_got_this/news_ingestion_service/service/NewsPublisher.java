package com.ai_got_this.news_ingestion_service.service;

import com.ai_got_this.news_ingestion_service.dto.NewsHeadline;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class NewsPublisher {
    private RabbitTemplate rabbitTemplate;

    public NewsPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishNews(String exchange, NewsHeadline news, String routingKey) {
        rabbitTemplate.convertAndSend(exchange, routingKey, news);
    }
}
