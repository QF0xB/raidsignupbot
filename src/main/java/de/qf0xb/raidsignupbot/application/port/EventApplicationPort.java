package de.qf0xb.raidsignupbot.application.port;

import de.qf0xb.raidsignupbot.domain.model.event.Event;

import java.util.Optional;
import java.util.Set;

public interface EventApplicationPort {
  Optional<Event> findById(Long id);
  Set<Event> findAllEvents();
  Set<Event> findAllUpcomingEvents();
  Set<Event> findAllChannelExistingEvents();
  Set<Event> findByTemplateKey(String templateKey);
  Optional<Event> findByChannelId(String channelId);
  Event save(Event event);
}
