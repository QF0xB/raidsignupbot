package de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.template;

import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.AbstractJpaApplicationAdapter;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventTemplateEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventTemplateEntityRepository;
import de.qf0xb.raidsignupbot.infrastructure.mapper.jpa.EventTemplateEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class JpaEventTemplateApplicationAdapter
    extends AbstractJpaApplicationAdapter<String, EventTemplate, EventTemplateEntity, EventTemplateEntityMapper, EventTemplateEntityRepository>
    implements EventTemplateApplicationPort {
  @Inject
  EventTemplateEntityMapper mapper;

  @Inject
  EventTemplateEntityRepository repository;

  @Override
  protected EventTemplateEntityMapper getMapper() {
    return mapper;
  }

  @Override
  protected EventTemplateEntityRepository getRepository() {
    return repository;
  }

  @Override
  public Optional<EventTemplate> findByKey(String templateKey) {
    return getRepository().findByIdOptional(templateKey)
        .map(getMapper()::toDomain);
  }

  @Override
  public EventTemplate save(EventTemplate eventTemplate) {
    return save(eventTemplate.getKey(), eventTemplate);
  }

  @Override
  public EventTemplateEntity partialUpdate(EventTemplate domain, EventTemplateEntity entity) {
    return getMapper().partialUpdate(domain, entity);
  }
}
