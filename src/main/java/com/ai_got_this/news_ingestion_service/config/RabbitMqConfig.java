package com.ai_got_this.news_ingestion_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public Queue financialNewsQueue() {
        return new Queue("financial-news", true);
    }

    @Bean
    public DirectExchange financialNewsExchange() {
        return new DirectExchange("financial-news-exchange");
    }

    @Bean
    public Binding bindingFinancialNewsQueue(Queue financialNewsQueue, DirectExchange financialNewsExchange) {
        return BindingBuilder.bind(financialNewsQueue).to(financialNewsExchange).with("financial-news");
    }
}