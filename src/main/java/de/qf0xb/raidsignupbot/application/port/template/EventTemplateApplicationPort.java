package de.qf0xb.raidsignupbot.application.port.template;

import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;

import java.util.Optional;

public interface EventTemplateApplicationPort {
  Optional<EventTemplate> findByKey(String templateKey);
  EventTemplate save(EventTemplate eventTemplate);
}
