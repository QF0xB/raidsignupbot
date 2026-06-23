package de.qf0xb.raidsignupbot.infrastructure.mapper.jpa;

import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventRoleEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventStrategyEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventUtilityRoleEntity;
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
    uses = EventRoleEntityMapper.class)
public abstract class EventStrategyEntityMapper implements JpaEntityMapper<EventStrategyEntity, EventStrategy> {
  @Inject
  EventRoleEntityMapper eventRoleEntityMapper;

  @Override
  @Mapping(target = "guides", ignore = true)
  @Mapping(target = "eventRoleEntities", ignore = true)
  @Mapping(target = "eventTemplateEntity", ignore = true)
  public abstract EventStrategyEntity toEntity(EventStrategy eventStrategy);

  @Override
  @Mapping(target = "guides", ignore = true)
  @Mapping(target = "primaryRoles", ignore = true)
  @Mapping(target = "utilityRoles", ignore = true)
  public abstract EventStrategy toDomain(EventStrategyEntity eventStrategyEntity);

  @Override
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "guides", ignore = true)
  @Mapping(target = "eventRoleEntities", ignore = true)
  @Mapping(target = "eventTemplateEntity", ignore = true)
  public abstract EventStrategyEntity partialUpdate(
      EventStrategy eventStrategy, @MappingTarget EventStrategyEntity eventStrategyEntity);

  @AfterMapping
  protected void populateEntity(EventStrategy source, @MappingTarget EventStrategyEntity target) {
    target.getGuides().clear();
    target.getGuides().addAll(source.getGuides());
    syncRoles(target, source);
  }

  @AfterMapping
  protected void populateDomain(EventStrategyEntity source, @MappingTarget EventStrategy target) {
    source.getGuides().forEach(target::addGuide);
    source.getEventRoleEntities().stream()
        .map(eventRoleEntityMapper::toDomain)
        .forEach(target::addRole);
  }

  private void syncRoles(EventStrategyEntity eventStrategyEntity, EventStrategy eventStrategy) {
    Map<String, EventRoleEntity> existingRolesByKey = new LinkedHashMap<>();
    for (EventRoleEntity roleEntity : eventStrategyEntity.getEventRoleEntities()) {
      existingRolesByKey.put(roleEntity.getKey(), roleEntity);
    }

    Set<EventRoleEntity> updatedRoles = new LinkedHashSet<>();
    for (EventRole role : eventStrategy.getPrimaryRoles()) {
      updatedRoles.add(syncRole(eventStrategyEntity, existingRolesByKey, role));
    }
    for (EventUtilityRole role : eventStrategy.getUtilityRoles()) {
      updatedRoles.add(syncRole(eventStrategyEntity, existingRolesByKey, role));
    }

    eventStrategyEntity.getEventRoleEntities().clear();
    eventStrategyEntity.getEventRoleEntities().addAll(updatedRoles);
  }

  private EventRoleEntity syncRole(
      EventStrategyEntity eventStrategyEntity,
      Map<String, EventRoleEntity> existingRolesByKey,
      EventRole role) {
    EventRoleEntity existing = existingRolesByKey.remove(role.getKey());
    EventRoleEntity updated;

    if (role instanceof EventUtilityRole utilityRole) {
      if (existing instanceof EventUtilityRoleEntity utilityRoleEntity) {
        updated = eventRoleEntityMapper.partialUpdate(utilityRole, utilityRoleEntity);
      } else {
        updated = eventRoleEntityMapper.toEntity(utilityRole);
        if (existing != null) {
          updated.setId(existing.getId());
        }
      }
    } else {
      if (existing != null && !(existing instanceof EventUtilityRoleEntity)) {
        updated = eventRoleEntityMapper.partialUpdate(role, existing);
      } else {
        updated = eventRoleEntityMapper.toEntity(role);
        if (existing != null) {
          updated.setId(existing.getId());
        }
      }
    }

    updated.setEventStrategyEntity(eventStrategyEntity);
    return updated;
  }
}
