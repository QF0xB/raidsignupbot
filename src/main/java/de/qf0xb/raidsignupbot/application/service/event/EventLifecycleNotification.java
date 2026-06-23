package de.qf0xb.raidsignupbot.application.service.event;

import java.time.Instant;

public record EventLifecycleNotification(
    Long eventId,
    String templateKey,
    EventLifecycleStage stage,
    Instant startAt,
    Instant endAt) {
}
