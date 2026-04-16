package com.schoolerp.workflow.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolerp.common.messaging.MessagingTopics;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "school-erp.messaging", name = "enabled", havingValue = "true")
public class WorkflowMessagingConfiguration {
    @Bean
    public TopicExchange domainEventsExchange() {
        return new TopicExchange(MessagingTopics.DOMAIN_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter workflowMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
