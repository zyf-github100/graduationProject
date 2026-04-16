package com.schoolerp.workflow.messaging;

import com.schoolerp.common.messaging.DomainEventMessage;
import com.schoolerp.common.messaging.MessagingTopics;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class WorkflowEventPublisher {
    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;
    private final boolean messagingEnabled;

    public WorkflowEventPublisher(ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
                                  @Value("${school-erp.messaging.enabled:false}") boolean messagingEnabled) {
        this.rabbitTemplateProvider = rabbitTemplateProvider;
        this.messagingEnabled = messagingEnabled;
    }

    public void publishTaskStatusChanged(Map<String, Object> taskSnapshot, String action, String opinion) {
        if (!messagingEnabled) {
            return;
        }

        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            return;
        }

        String normalizedAction = action == null ? "unknown" : action.trim().toLowerCase();
        String routingKey = "workflow." + normalizedAction;
        DomainEventMessage event = new DomainEventMessage(
                "WorkflowTaskStatusChanged",
                routingKey,
                "workflow-service",
                stringValue(taskSnapshot.get("bizType")),
                stringValue(taskSnapshot.get("processNo")),
                "审批状态变更",
                opinion == null || opinion.isBlank() ? "流程状态已变更。" : opinion,
                OffsetDateTime.now(),
                Map.of(
                        "taskId", taskSnapshot.get("id"),
                        "status", taskSnapshot.get("status"),
                        "currentNode", taskSnapshot.get("currentNode"),
                        "applicantName", taskSnapshot.get("applicantName"),
                        "className", taskSnapshot.get("className"),
                        "action", normalizedAction
                )
        );

        rabbitTemplate.convertAndSend(MessagingTopics.DOMAIN_EVENTS_EXCHANGE, routingKey, event);
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
