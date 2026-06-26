package de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.guild;

import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.AbstractJpaApplicationAdapter;
import de.qf0xb.raidsignupbot.infrastructure.jpa.guild.GuildEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.guild.GuildEntityRepository;
import de.qf0xb.raidsignupbot.infrastructure.mapper.jpa.GuildEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaGuildApplicationAdapter
    extends AbstractJpaApplicationAdapter<Long, Guild, GuildEntity, GuildEntityMapper, GuildEntityRepository>
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
  @Transactional
  public Optional<Guild> findById(Long id) {
    return getRepository().findByIdOptional(id)
        .map(getMapper()::toDomain);
  }

  @Override
  @Transactional
  public Set<Guild> findGuildByServerId(String serverId) {
    return getRepository().find("server.serverId", serverId).stream()
        .map(getMapper()::toDomain)
        .collect(Collectors.toSet());
  }

  @Override
  @Transactional
  public Optional<Guild> findByServerIdAndName(String serverId, String name) {
    return getRepository().find("server.serverId = ?1 and name = ?2", serverId, name).firstResultOptional()
        .map(getMapper()::toDomain);
  }

  @Override
  @Transactional
  public Optional<Guild> findByName(String name) {
    return getRepository().find("name", name).firstResultOptional()
        .map(getMapper()::toDomain);
  }


  @Override
  @Transactional
  public Guild save(Guild guild) {
    Optional<GuildEntity> existing = getRepository()
        .find("server.serverId = ?1 and name = ?2", guild.getServerId(), guild.getName())
        .firstResultOptional();

    if (existing.isPresent()) {
      return save(existing.get().getId(), guild);
    }
    return save(null, guild);
  }

  @Override
  @Transactional
  public GuildEntity partialUpdate(Guild domain, GuildEntity entity) {
    return getMapper().partialUpdate(domain, entity);
  }
}
