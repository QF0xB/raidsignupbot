package de.qf0xb.raidsignupbot.infrastructure.mapper.jpa;

import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.infrastructure.jpa.GuildEntity;
import org.mapstruct.*;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.CDI)
public interface GuildEntityMapper extends JpaEntityMapper<GuildEntity, Guild>{
  GuildEntity toEntity(Guild guild);

  Guild toDomain(GuildEntity guildEntity);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  GuildEntity partialUpdate(Guild guild, @MappingTarget GuildEntity guildEntity);
}
