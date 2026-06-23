package de.qf0xb.raidsignupbot.infrastructure.mapper.jpa;

import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventRoleEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventUtilityRoleEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.SubclassMapping;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface EventRoleEntityMapper extends JpaEntityMapper<EventRoleEntity, EventRole> {
  @Override
  @Mapping(target = "eventStrategyEntity", ignore = true)
  @SubclassMapping(source = EventUtilityRole.class, target = EventUtilityRoleEntity.class)
  EventRoleEntity toEntity(EventRole eventRole);

  @SubclassMapping(source = EventUtilityRoleEntity.class, target = EventUtilityRole.class)
  EventRole toDomain(EventRoleEntity eventRoleEntity);

  @Override
  @Mapping(target = "eventStrategyEntity", ignore = true)
  EventRoleEntity partialUpdate(EventRole eventRole, @MappingTarget EventRoleEntity eventRoleEntity);

  @Mapping(target = "eventStrategyEntity", ignore = true)
  @Mapping(target = "eligiblePrimaryRoleKeys", source = "eligiblePrimaryRoleKeys")
  EventUtilityRoleEntity toEntity(EventUtilityRole eventUtilityRole);

  @Mapping(target = "eligiblePrimaryRoleKeys", ignore = true)
  EventUtilityRole toDomain(EventUtilityRoleEntity eventUtilityRoleEntity);

  @Mapping(target = "eventStrategyEntity", ignore = true)
  @Mapping(target = "eligiblePrimaryRoleKeys", source = "eligiblePrimaryRoleKeys")
  EventUtilityRoleEntity partialUpdate(
      EventUtilityRole eventUtilityRole, @MappingTarget EventUtilityRoleEntity eventUtilityRoleEntity);

  @AfterMapping
  default void copyEligiblePrimaryRoleKeys(
      EventUtilityRoleEntity source, @MappingTarget EventUtilityRole target) {
    source.getEligiblePrimaryRoleKeys().forEach(target::addEligiblePrimaryRole);
  }

  @AfterMapping
  default void copyEligiblePrimaryRoleKeys(
      EventUtilityRole source, @MappingTarget EventUtilityRoleEntity target) {
    target.getEligiblePrimaryRoleKeys().clear();
    target.getEligiblePrimaryRoleKeys().addAll(source.getEligiblePrimaryRoleKeys());
  }
}
