package com.schoolerp.common.messaging;

public final class MessagingTopics {
    public static final String DOMAIN_EVENTS_EXCHANGE = "school.erp.domain.events";
    public static final String WORKFLOW_NOTIFICATIONS_QUEUE = "school.erp.notify.workflow.queue";
    public static final String WORKFLOW_ROUTING_PATTERN = "workflow.#";

    private MessagingTopics() {
    }
}
