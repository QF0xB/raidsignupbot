package de.qf0xb.raidsignupbot.guild.infrastructure.adapter.out.jpa;

import de.qf0xb.raidsignupbot.guild.application.port.GuildPort;
import de.qf0xb.raidsignupbot.guild.domain.model.Guild;
import de.qf0xb.raidsignupbot.guild.infrastructure.mapper.GuildJpaMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class GuildJpaAdapter implements GuildPort {
  private final GuildJpaRepository repository;
  private final GuildJpaMapper mapper;

  public GuildJpaAdapter(GuildJpaRepository repository, GuildJpaMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<Guild> findByGuildId(String guildId) {
    return repository.findByIdOptional(guildId)
        .map(mapper::toDomain);
  }

  @Override
  @Transactional
  public Guild save(Guild guild) {
    GuildEntity entity = mapper.toEntity(guild);
    GuildEntity saved = repository.getEntityManager().merge(entity);
    return mapper.toDomain(saved);
  }
}
