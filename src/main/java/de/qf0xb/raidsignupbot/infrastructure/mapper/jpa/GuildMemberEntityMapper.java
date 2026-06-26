package de.qf0xb.raidsignupbot.infrastructure.mapper.jpa;

import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.infrastructure.jpa.guild.GuildEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.guild.GuildEntityRepository;
import de.qf0xb.raidsignupbot.infrastructure.jpa.guild.GuildMemberEntity;
import jakarta.inject.Inject;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
    uses = GuildEntityMapper.class)
public abstract class GuildMemberEntityMapper implements JpaEntityMapper<GuildMemberEntity, GuildMember> {
  @Inject
  GuildEntityMapper guildEntityMapper;

  @Inject
  GuildEntityRepository guildEntityRepository;

  @Override
  @Mapping(target = "activeGuilds", ignore = true)
  public abstract GuildMemberEntity toEntity(GuildMember guildMember);

  @Override
  @Mapping(target = "activeGuilds", ignore = true)
  public abstract GuildMember toDomain(GuildMemberEntity guildMemberEntity);

  @Override
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "activeGuilds", ignore = true)
  public abstract GuildMemberEntity partialUpdate(
      GuildMember guildMember, @MappingTarget GuildMemberEntity guildMemberEntity);

  @AfterMapping
  protected void copyActiveGuilds(GuildMember source, @MappingTarget GuildMemberEntity target) {
    target.getActiveGuilds().clear();
    for (Guild guild : source.getActiveGuilds()) {
      GuildEntity guildEntity = guildEntityRepository
          .find("server.serverId = ?1 and name = ?2", guild.getServerId(), guild.getName())
          .firstResultOptional()
          .orElseThrow(() -> new IllegalArgumentException(
              "Guild with name " + guild.getName() + " not found for server " + guild.getServerId()));
      target.getActiveGuilds().add(guildEntity);
    }
  }

  @AfterMapping
  protected void copyActiveGuilds(GuildMemberEntity source, @MappingTarget GuildMember target) {
    for (Guild guild : source.getActiveGuilds().stream().map(guildEntityMapper::toDomain).toList()) {
      target.addActiveGuild(guild);
    }
  }
}
