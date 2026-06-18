package de.qf0xb.raidsignupbot.signup.infrastructure.mapper;

import de.qf0xb.raidsignupbot.shared.domain.model.PlayerProfile;
import de.qf0xb.raidsignupbot.signup.infrastructure.adapter.out.persistence.PlayerProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface PlayerProfilePersistenceMapper {
  PlayerProfile toDomain(PlayerProfileEntity entity);

  @Mapping(target = "id", ignore = true)
  PlayerProfileEntity toEntity(PlayerProfile domain);

  @Mapping(target = "id", ignore = true)
  void updateEntity(PlayerProfile domain, @MappingTarget PlayerProfileEntity entity);
}
