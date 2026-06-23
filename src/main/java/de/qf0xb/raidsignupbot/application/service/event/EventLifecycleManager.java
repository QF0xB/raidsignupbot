package de.qf0xb.raidsignupbot.application.service.event;

import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.port.EventSignupChannelApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class EventLifecycleManager {
  private static final Logger LOG = Logger.getLogger(EventLifecycleManager.class);

  private final EventApplicationPort eventApplicationPort;
  private final EventService eventService;
  private final Instance<EventSignupChannelApplicationPort> eventSignupApplicationPort;
  private final jakarta.enterprise.event.Event<EventLifecycleNotification> lifecycleNotificationEvent;

  public EventLifecycleManager(
      EventApplicationPort eventApplicationPort,
      EventService eventService,
      Instance<EventSignupChannelApplicationPort> eventSignupApplicationPort,
      jakarta.enterprise.event.Event<EventLifecycleNotification> lifecycleNotificationEvent) {
    this.eventApplicationPort = eventApplicationPort;
    this.eventService = eventService;
    this.eventSignupApplicationPort = eventSignupApplicationPort;
    this.lifecycleNotificationEvent = lifecycleNotificationEvent;
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
      if (eventSignupApplicationPort.isResolvable()) {
        eventSignupApplicationPort.get().createSignupChannel(event);
      }
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
    if (shouldCloseSignup(event, now)) {
      closeSignupIfPossible(event);
      return;
    }

    if (shouldEmit(event, now, Duration.ofHours(1))
        && !event.isOneHourBeforeEmitted()) {
      emit(event, EventLifecycleStage.ONE_HOUR_BEFORE);
      event.markOneHourBeforeEmitted();
      eventApplicationPort.save(event);
    }

    if (shouldEmit(event, now, Duration.ofMinutes(30))
        && !event.isThirtyMinutesBeforeEmitted()) {
      emit(event, EventLifecycleStage.THIRTY_MINUTES_BEFORE);
      event.markThirtyMinutesBeforeEmitted();
      eventApplicationPort.save(event);
    }

    if (shouldEmit(event, now, Duration.ofMinutes(15))
        && !event.isFifteenMinutesBeforeEmitted()) {
      emit(event, EventLifecycleStage.FIFTEEN_MINUTES_BEFORE);
      event.markFifteenMinutesBeforeEmitted();
      eventApplicationPort.save(event);
    }

    if (shouldEmitAfter(event, now)
        && !event.isOneHourAfterEmitted()) {
      emit(event, EventLifecycleStage.ONE_HOUR_AFTER);
      event.markOneHourAfterEmitted();
      eventApplicationPort.save(event);
    }
  }

  private boolean shouldCloseSignup(Event event, Instant now) {
    return event.isSignupOpen()
        && event.getStart() != null
        && !now.isBefore(event.getStart().minus(Duration.ofDays(1)));
  }

  private void closeSignupIfPossible(Event event) {
    if (!eventSignupApplicationPort.isResolvable()) {
      LOG.debugf(
          "Skipping team generation for event %s because no signup source port is available yet",
          event.getId());
      return;
    }

    List<EventMemberSignup> signups = eventSignupApplicationPort.get().getSignupsByMessageId(event);
    eventService.generateTeamsAndCloseSignup(event.getId(), signups);
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
            event.getId(),
            event.getTemplate().getKey(),
            stage,
            event.getStart(),
            event.getEnd()));
  }
}
