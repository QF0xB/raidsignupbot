package de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.template;

import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.AbstractJpaApplicationAdapter;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventTemplateEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventTemplateEntityRepository;
import de.qf0xb.raidsignupbot.infrastructure.mapper.jpa.EventTemplateEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
  @Transactional
  public Optional<EventTemplate> findByServerIdAndKey(String serverId, String templateKey) {
    return getRepository().findByIdOptional(templateKey)
        .filter(template -> template.getServer() != null && serverId.equals(template.getServer().getServerId()))
        .map(getMapper()::toDomain);
  }

  @Override
  @Transactional
  public Set<EventTemplate> findAllByServerId(String serverId) {
    return getRepository().find("server.serverId", serverId).stream()
        .map(getMapper()::toDomain)
        .collect(Collectors.toSet());
  }

  @Override
  @Transactional
  public EventTemplate save(EventTemplate eventTemplate) {
    return save(eventTemplate.getKey(), eventTemplate);
  }

  @Override
  @Transactional
  public EventTemplateEntity partialUpdate(EventTemplate domain, EventTemplateEntity entity) {
    return getMapper().partialUpdate(domain, entity);
  }
}
