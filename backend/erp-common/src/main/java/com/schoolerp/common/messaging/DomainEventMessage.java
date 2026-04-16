package com.schoolerp.common.messaging;

import java.time.OffsetDateTime;
import java.util.Map;

public record DomainEventMessage(
        String eventType,
        String routingKey,
        String sourceService,
        String bizType,
        String bizId,
        String title,
        String detail,
        OffsetDateTime occurredAt,
        Map<String, Object> payload
) {
}
