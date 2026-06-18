package de.qf0xb.raidsignupbot.guild.infrastructure.mapper;

import de.qf0xb.raidsignupbot.guild.domain.model.GuildMember;
import de.qf0xb.raidsignupbot.guild.infrastructure.adapter.out.jpa.GuildMemberEntity;
import org.mapstruct.*;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.CDI)
public interface GuildMemberJpaMapper {
  GuildMemberEntity toEntity(GuildMember guildMember);

  GuildMember toDomain(GuildMemberEntity guildMemberEntity);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  GuildMemberEntity partialUpdate(
      GuildMember guildMember, @MappingTarget GuildMemberEntity guildMemberEntity);
}
