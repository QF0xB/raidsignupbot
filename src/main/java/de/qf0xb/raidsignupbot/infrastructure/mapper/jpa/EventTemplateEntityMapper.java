package de.qf0xb.raidsignupbot.infrastructure.mapper.jpa;

import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.infrastructure.jpa.server.ServerEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.server.ServerEntityRepository;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventStrategyEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventTemplateEntity;
import jakarta.inject.Inject;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
    uses = EventStrategyEntityMapper.class)
public abstract class EventTemplateEntityMapper implements JpaEntityMapper<EventTemplateEntity, EventTemplate> {
  @Inject
  EventStrategyEntityMapper eventStrategyEntityMapper;

  @Inject
  ServerEntityRepository serverEntityRepository;

  @Override
  @Mapping(target = "eventStrategies", ignore = true)
  @Mapping(target = "server", ignore = true)
  public abstract EventTemplateEntity toEntity(EventTemplate eventTemplate);

  @Override
  @Mapping(target = "strategies", ignore = true)
  @Mapping(target = "server", ignore = true)
  @Mapping(target = "serverId", source = "server.serverId")
  public abstract EventTemplate toDomain(EventTemplateEntity eventTemplateEntity);

  @Override
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "eventStrategies", ignore = true)
  @Mapping(target = "server", ignore = true)
  public abstract EventTemplateEntity partialUpdate(
      EventTemplate eventTemplate, @MappingTarget EventTemplateEntity eventTemplateEntity);

  @AfterMapping
  protected void populateEntity(EventTemplate source, @MappingTarget EventTemplateEntity target) {
    target.setServer(resolveServer(source));
    target.setEmojiSlug(source.getEmojiSlug());
    syncStrategies(target, source);
  }

  @AfterMapping
  protected void populateDomain(EventTemplateEntity source, @MappingTarget EventTemplate target) {
    if (source.getServer() != null) {
      target.setServerId(source.getServer().getServerId());
    }
    target.setEmojiSlug(source.getEmojiSlug());
    source.getEventStrategies().stream()
        .map(eventStrategyEntityMapper::toDomain)
        .forEach(target::addStrategy);
  }

  private void syncStrategies(EventTemplateEntity eventTemplateEntity, EventTemplate eventTemplate) {
    Map<String, EventStrategyEntity> existingStrategiesByKey = new LinkedHashMap<>();
    for (EventStrategyEntity strategyEntity : eventTemplateEntity.getEventStrategies()) {
      existingStrategiesByKey.put(strategyEntity.getKey(), strategyEntity);
    }

    Set<EventStrategyEntity> updatedStrategies = new LinkedHashSet<>();
    for (EventStrategy strategy : eventTemplate.getStrategies()) {
      updatedStrategies.add(syncStrategy(eventTemplateEntity, existingStrategiesByKey, strategy));
    }

    eventTemplateEntity.getEventStrategies().clear();
    eventTemplateEntity.getEventStrategies().addAll(updatedStrategies);
  }

  private EventStrategyEntity syncStrategy(
      EventTemplateEntity eventTemplateEntity,
      Map<String, EventStrategyEntity> existingStrategiesByKey,
      EventStrategy strategy) {
    EventStrategyEntity existing = existingStrategiesByKey.remove(strategy.getKey());
    EventStrategyEntity updated;

    if (existing != null) {
      updated = eventStrategyEntityMapper.partialUpdate(strategy, existing);
      updated.setId(existing.getId());
    } else {
      updated = eventStrategyEntityMapper.toEntity(strategy);
    }

    updated.setEventTemplateEntity(eventTemplateEntity);
    return updated;
  }

  private ServerEntity resolveServer(EventTemplate source) {
    if (source.getServer() == null) {
      return null;
    }

    return serverEntityRepository.findByIdOptional(source.getServerId())
        .orElseThrow(() -> new IllegalArgumentException("Server with id " + source.getServerId() + " not found"));
  }
}
