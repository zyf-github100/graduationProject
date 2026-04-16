package com.schoolerp.notify.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolerp.common.messaging.MessagingTopics;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
@ConditionalOnProperty(prefix = "school-erp.messaging", name = "enabled", havingValue = "true")
public class NotifyMessagingConfiguration {
    @Bean
    public TopicExchange domainEventsExchange() {
        return new TopicExchange(MessagingTopics.DOMAIN_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue workflowNotificationsQueue() {
        return new Queue(MessagingTopics.WORKFLOW_NOTIFICATIONS_QUEUE, true);
    }

    @Bean
    public Binding workflowNotificationsBinding(Queue workflowNotificationsQueue, TopicExchange domainEventsExchange) {
        return BindingBuilder.bind(workflowNotificationsQueue)
                .to(domainEventsExchange)
                .with(MessagingTopics.WORKFLOW_ROUTING_PATTERN);
    }

    @Bean
    public MessageConverter notifyMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
