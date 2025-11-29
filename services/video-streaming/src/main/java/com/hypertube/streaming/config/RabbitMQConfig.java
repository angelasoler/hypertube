package com.hypertube.streaming.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queues.download}")
    private String downloadQueue;

    @Value("${rabbitmq.queues.conversion}")
    private String conversionQueue;

    @Value("${rabbitmq.queues.subtitle}")
    private String subtitleQueue;

    @Value("${rabbitmq.exchanges.video}")
    private String videoExchange;

    @Value("${rabbitmq.routing-keys.download}")
    private String downloadRoutingKey;

    @Value("${rabbitmq.routing-keys.conversion}")
    private String conversionRoutingKey;

    @Value("${rabbitmq.routing-keys.subtitle}")
    private String subtitleRoutingKey;

    @Bean
    public Queue downloadQueue() {
        return QueueBuilder.durable(downloadQueue)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .withArgument("x-max-priority", 10) // Enable priority queue
                .build();
    }

    @Bean
    public Queue conversionQueue() {
        return QueueBuilder.durable(conversionQueue)
                .withArgument("x-message-ttl", 7200000) // 2 hours TTL
                .build();
    }

    @Bean
    public Queue subtitleQueue() {
        return QueueBuilder.durable(subtitleQueue)
                .withArgument("x-message-ttl", 1800000) // 30 minutes TTL
                .build();
    }

    @Bean
    public TopicExchange videoExchange() {
        return new TopicExchange(videoExchange);
    }

    @Bean
    public Binding downloadBinding(Queue downloadQueue, TopicExchange videoExchange) {
        return BindingBuilder.bind(downloadQueue)
                .to(videoExchange)
                .with(downloadRoutingKey);
    }

    @Bean
    public Binding conversionBinding(Queue conversionQueue, TopicExchange videoExchange) {
        return BindingBuilder.bind(conversionQueue)
                .to(videoExchange)
                .with(conversionRoutingKey);
    }

    @Bean
    public Binding subtitleBinding(Queue subtitleQueue, TopicExchange videoExchange) {
        return BindingBuilder.bind(subtitleQueue)
                .to(videoExchange)
                .with(subtitleRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
