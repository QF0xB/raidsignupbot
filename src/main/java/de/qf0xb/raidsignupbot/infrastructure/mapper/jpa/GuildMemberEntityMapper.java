package de.qf0xb.raidsignupbot.infrastructure.mapper.jpa;

import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.infrastructure.jpa.GuildMemberEntity;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface GuildMemberEntityMapper extends JpaEntityMapper<GuildMemberEntity, GuildMember> {

  GuildMemberEntity toEntity(GuildMember guildMember);

  GuildMember toDomain(GuildMemberEntity guildMemberEntity);

  GuildMemberEntity partialUpdate(
      GuildMember guildMember, @MappingTarget GuildMemberEntity guildMemberEntity);
}
