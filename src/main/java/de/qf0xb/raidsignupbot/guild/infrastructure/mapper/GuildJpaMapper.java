package de.qf0xb.raidsignupbot.guild.infrastructure.mapper;

import de.qf0xb.raidsignupbot.guild.domain.model.Guild;
import de.qf0xb.raidsignupbot.guild.infrastructure.adapter.out.jpa.GuildEntity;
import org.mapstruct.*;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.CDI)
public interface GuildJpaMapper {
  GuildEntity toEntity(Guild guild);

  Guild toDomain(GuildEntity guildEntity);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  GuildEntity partialUpdate(Guild guild, @MappingTarget GuildEntity guildEntity);
}
