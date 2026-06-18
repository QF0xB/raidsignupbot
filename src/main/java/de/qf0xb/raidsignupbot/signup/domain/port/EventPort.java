package de.qf0xb.raidsignupbot.signup.domain.port;

import de.qf0xb.raidsignupbot.signup.domain.model.Event;
import de.qf0xb.raidsignupbot.template.domain.model.EventTemplate;

import java.util.List;

public interface EventPort {
  List<Event> getAllByGuildId(String guildId);
  List<Event> getByTemplateId(Long templateId);
  List<Event> getByTemplate(EventTemplate template);
  Event getById(Long id);
  void save(Event event);
}
