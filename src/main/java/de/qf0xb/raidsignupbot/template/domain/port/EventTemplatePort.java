package de.qf0xb.raidsignupbot.template.domain.port;

import de.qf0xb.raidsignupbot.template.domain.model.EventTemplate;

import java.util.Optional;

public interface EventTemplatePort {
  Optional<EventTemplate> getByStub(String stub);
  void save(EventTemplate template);
}
