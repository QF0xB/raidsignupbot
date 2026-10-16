package de.qf0xb.raidsignupbot.infrastructure.mapper.jpa;

import de.qf0xb.raidsignupbot.domain.model.guild.RaiderRoleGrant;
import de.qf0xb.raidsignupbot.infrastructure.jpa.guild.RaiderRoleGrantEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface RaiderRoleGrantEntityMapper extends JpaEntityMapper<RaiderRoleGrantEntity, RaiderRoleGrant> {
  RaiderRoleGrantEntity toEntity(RaiderRoleGrant grant);

  RaiderRoleGrant toDomain(RaiderRoleGrantEntity entity);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  RaiderRoleGrantEntity partialUpdate(RaiderRoleGrant grant, @MappingTarget RaiderRoleGrantEntity entity);
}
