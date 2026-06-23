package de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.guild;

import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.AbstractJpaApplicationAdapter;
import de.qf0xb.raidsignupbot.infrastructure.jpa.guild.GuildEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.guild.GuildEntityRepository;
import de.qf0xb.raidsignupbot.infrastructure.mapper.jpa.GuildEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class JpaGuildApplicationAdapter
    extends AbstractJpaApplicationAdapter<String, Guild, GuildEntity, GuildEntityMapper, GuildEntityRepository>
    implements GuildApplicationPort {
  @Inject
  GuildEntityMapper mapper;

  @Inject
  GuildEntityRepository repository;

  @Override
  protected GuildEntityMapper getMapper() {
    return mapper;
  }

  @Override
  protected GuildEntityRepository getRepository() {
    return repository;
  }

  @Override
  public Optional<Guild> findById(String guildId) {
    return getRepository().findByIdOptional(guildId)
        .map(getMapper()::toDomain);
  }

  @Override
  public Guild save(Guild guild) {
    return save(guild.getGuildId(), guild);
  }

  @Override
  public GuildEntity partialUpdate(Guild domain, GuildEntity entity) {
    return getMapper().partialUpdate(domain, entity);
  }
}
