package de.qf0xb.raidsignupbot.infrastructure.mapper.jpa;

import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.infrastructure.jpa.guild.GuildEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.server.ServerEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.server.ServerEntityRepository;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import jakarta.inject.Inject;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public abstract class GuildEntityMapper implements JpaEntityMapper<GuildEntity, Guild> {
  @Inject
  ServerEntityRepository serverEntityRepository;

  @Override
  @Mapping(target = "server", ignore = true)
  public abstract GuildEntity toEntity(Guild guild);

  @Override
  @Mapping(target = "serverId", source = "server.serverId")
  public abstract Guild toDomain(GuildEntity guildEntity);

  @Override
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "server", ignore = true)
  public abstract GuildEntity partialUpdate(Guild guild, @MappingTarget GuildEntity guildEntity);

  @AfterMapping
  protected void populateEntity(Guild source, @MappingTarget GuildEntity target) {
    target.setServer(resolveServer(source));
  }

  @AfterMapping
  protected void populateDomain(GuildEntity source, @MappingTarget Guild target) {
    if (source.getServer() != null) {
      target.setServerId(source.getServer().getServerId());
    }
  }

  private ServerEntity resolveServer(Guild source) {
    if (source.getServer() == null) {
      return null;
    }

    return serverEntityRepository.findByIdOptional(source.getServerId())
        .orElseThrow(() -> new IllegalArgumentException("Server with id " + source.getServerId() + " not found"));
  }
}
