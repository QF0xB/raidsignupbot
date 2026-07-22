package de.qf0xb.raidsignupbot.application.service.event;

import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.adapter.DiscordEventUIApplicationAdapter;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@ApplicationScoped
public class EventLifecycleManager {
  private static final Logger LOG = Logger.getLogger(EventLifecycleManager.class);

  private final EventApplicationPort eventApplicationPort;
  private final EventService eventService;
  private final jakarta.enterprise.event.Event<EventLifecycleNotification>
      lifecycleNotificationEvent;
  private final DiscordEventUIApplicationAdapter discordEventUIApplicationAdapter;

  public EventLifecycleManager(
      EventApplicationPort eventApplicationPort,
      EventService eventService,
      jakarta.enterprise.event.Event<EventLifecycleNotification> lifecycleNotificationEvent,
      DiscordEventUIApplicationAdapter discordEventUIApplicationAdapter) {
    this.eventApplicationPort = eventApplicationPort;
    this.eventService = eventService;
    this.lifecycleNotificationEvent = lifecycleNotificationEvent;
    this.discordEventUIApplicationAdapter = discordEventUIApplicationAdapter;
  }

  @Scheduled(every = "1m", identity = "event-lifecycle-manager")
  @Transactional
  void runLifecycleChecks() {
    LOG.info("Running event lifecycle checks");
    Instant now = Instant.now();
    Map<Long, Event> candidates = new LinkedHashMap<>();

    collectCandidates(candidates, eventApplicationPort.findAllUpcomingEvents());
    collectCandidates(candidates, eventApplicationPort.findAllChannelExistingEvents());

    for (Event event : candidates.values()) {
      LOG.infof("Processing event %s", event.getId());
      processEvent(event, now);
    }
  }

  private void collectCandidates(Map<Long, Event> candidates, Set<Event> events) {
    for (Event event : events) {
      if (event.getId() != null) {
        candidates.put(event.getId(), event);
      }
    }
  }

  private void processEvent(Event event, Instant now) {
    log.info(
        "For event {}: Should close signup: {}, Should emit 1h before: {}, Should emit 30m before: {}, Should emit 15m before: {}, Should emit 1h after:{}",
        event,
        shouldCloseSignup(event, now),
        shouldEmit(event, now, Duration.ofHours(1)),
        shouldEmit(event, now, Duration.ofMinutes(30)),
        shouldEmit(event, now, Duration.ofMinutes(15)),
        shouldEmitAfter(event, now));


    if (shouldCloseSignup(event, now)) {
      closeSignupIfPossible(event);
      return;
    }

    emitIfDue(event, now, Duration.ofHours(1), event.isOneHourBeforeEmitted(), EventLifecycleStage.ONE_HOUR_BEFORE);
    emitIfDue(event, now, Duration.ofMinutes(30), event.isThirtyMinutesBeforeEmitted(), EventLifecycleStage.THIRTY_MINUTES_BEFORE);
    emitIfDue(event, now, Duration.ofMinutes(15), event.isFifteenMinutesBeforeEmitted(), EventLifecycleStage.FIFTEEN_MINUTES_BEFORE);
    emitAfterIfDue(event, now);
  }

  private boolean shouldCloseSignup(Event event, Instant now) {
    return event.isSignupOpen()
        && event.getStart() != null
        && !now.isBefore(event.getStart().minus(Duration.ofDays(1)));
  }

  private void closeSignupIfPossible(Event event) {
    eventService.generateTeamsAndCloseSignup(event.getId(), discordEventUIApplicationAdapter.closeSignup(event));
  }

  private boolean shouldEmit(Event event, Instant now, Duration offset) {
    return event.getStart() != null && !now.isBefore(event.getStart().minus(offset));
  }

  private boolean shouldEmitAfter(Event event, Instant now) {
    return event.getEnd() != null && !now.isBefore(event.getEnd().plus(Duration.ofHours(1)));
  }

  private void emit(Event event, EventLifecycleStage stage) {
    lifecycleNotificationEvent.fire(
        new EventLifecycleNotification(
            event.getId(), event.getTemplate().getKey(), stage, event.getStart(), event.getEnd()));
  }

  private void emitIfDue(Event event, Instant now, Duration offset, boolean alreadyEmitted, EventLifecycleStage stage) {
    if (!shouldEmit(event, now, offset) || alreadyEmitted) {
      return;
    }

    try {
      emit(event, stage);
      markEmitted(event, stage);
      eventApplicationPort.save(event);
    } catch (RuntimeException ex) {
      LOG.warnf(ex, "Failed to emit %s reminder for event %s", stage, event.getId());
    }
  }

  private void emitAfterIfDue(Event event, Instant now) {
    if (!shouldEmitAfter(event, now) || event.isOneHourAfterEmitted()) {
      return;
    }

    try {
      emit(event, EventLifecycleStage.ONE_HOUR_AFTER);
      event.markOneHourAfterEmitted();
      eventApplicationPort.save(event);
    } catch (RuntimeException ex) {
      LOG.warnf(ex, "Failed to emit %s reminder for event %s", EventLifecycleStage.ONE_HOUR_AFTER, event.getId());
    }
  }

  private void markEmitted(Event event, EventLifecycleStage stage) {
    switch (stage) {
      case ONE_HOUR_BEFORE -> event.markOneHourBeforeEmitted();
      case THIRTY_MINUTES_BEFORE -> event.markThirtyMinutesBeforeEmitted();
      case FIFTEEN_MINUTES_BEFORE -> event.markFifteenMinutesBeforeEmitted();
      case ONE_HOUR_AFTER -> event.markOneHourAfterEmitted();
    }
  }
}
