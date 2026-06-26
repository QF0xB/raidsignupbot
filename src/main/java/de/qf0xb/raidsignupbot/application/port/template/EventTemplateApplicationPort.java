package de.qf0xb.raidsignupbot.application.port.template;

import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;

import java.util.Optional;
import java.util.Set;

public interface EventTemplateApplicationPort {
  Optional<EventTemplate> findByServerIdAndKey(String serverId, String templateKey);
  Set<EventTemplate> findAllByServerId(String serverId);
  EventTemplate save(EventTemplate eventTemplate);
}
