package de.qf0xb.raidsignupbot.guild.infrastructure.adapter.out.jpa;

import de.qf0xb.raidsignupbot.guild.application.port.GuildMemberPort;
import de.qf0xb.raidsignupbot.guild.domain.model.Guild;
import de.qf0xb.raidsignupbot.guild.domain.model.GuildMember;
import de.qf0xb.raidsignupbot.guild.infrastructure.mapper.GuildMemberJpaMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class GuildMemberJpaAdapter implements GuildMemberPort {
  private final GuildMemberJpaRepository repository;
  private final GuildMemberJpaMapper mapper;

  public GuildMemberJpaAdapter(GuildMemberJpaRepository repository, GuildMemberJpaMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<GuildMember> findByUserIdAndGuild(String userId, Guild guild) {
    return repository.find("guild.guildId = ?1 and playerId = ?2", guild.getGuildId(), userId)
        .firstResultOptional()
        .map(mapper::toDomain);
  }

  @Override
  public GuildMember save(GuildMember member) {
    GuildMemberEntity entity = mapper.toEntity(member);
    GuildMemberEntity saved = repository.getEntityManager().merge(entity);
    return mapper.toDomain(saved);
  }
}
