package de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa;

import de.qf0xb.raidsignupbot.application.event.port.GuildApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.infrastructure.jpa.GuildEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.GuildEntityRepository;
import de.qf0xb.raidsignupbot.infrastructure.mapper.jpa.GuildEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class JpaGuildApplicationAdapter
    extends AbstractJpaApplicationAdapter<String, Guild, GuildEntity, GuildEntityMapper, GuildEntityRepository>
    implements GuildApplicationPort {

  @Override
  public Optional<Guild> findById(String guildId) {
    return getRepository().findByIdOptional(guildId)
        .map(getMapper()::toDomain);
  }

  @Override
  public Guild save(Guild guild) {
    return save(guild.getGuildId(), guild);
  }
}
