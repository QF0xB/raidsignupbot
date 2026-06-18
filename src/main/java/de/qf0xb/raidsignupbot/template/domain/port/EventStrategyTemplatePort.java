package de.qf0xb.raidsignupbot.template.domain.port;

import de.qf0xb.raidsignupbot.template.domain.model.EventStrategyTemplate;
import de.qf0xb.raidsignupbot.template.infrastructure.adapter.out.persistence.EventStrategyTemplateEntity;

import java.util.List;
import java.util.Optional;

public interface EventStrategyTemplatePort {
  Optional<EventStrategyTemplate> getByStub(String stub);
  List<EventStrategyTemplate> getByEventTemplateStub(String eventTemplateStub);
  void save(EventStrategyTemplate template);
}
