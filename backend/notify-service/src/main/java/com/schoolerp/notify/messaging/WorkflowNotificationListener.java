package com.schoolerp.notify.messaging;

import com.schoolerp.common.messaging.DomainEventMessage;
import com.schoolerp.common.messaging.MessagingTopics;
import com.schoolerp.notify.service.NotifyTemplateService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "school-erp.messaging", name = "enabled", havingValue = "true")
public class WorkflowNotificationListener {
    private final NotifyTemplateService notifyTemplateService;

    public WorkflowNotificationListener(NotifyTemplateService notifyTemplateService) {
        this.notifyTemplateService = notifyTemplateService;
    }

    @RabbitListener(queues = MessagingTopics.WORKFLOW_NOTIFICATIONS_QUEUE)
    public void consumeWorkflowEvent(DomainEventMessage message) {
        notifyTemplateService.acceptWorkflowEvent(message);
    }
}
