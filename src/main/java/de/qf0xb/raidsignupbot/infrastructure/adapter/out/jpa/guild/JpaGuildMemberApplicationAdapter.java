package de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.guild;

import de.qf0xb.raidsignupbot.application.port.guild.GuildMemberApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.AbstractJpaApplicationAdapter;
import de.qf0xb.raidsignupbot.infrastructure.jpa.guild.GuildMemberEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.guild.GuildMemberEntityRepository;
import de.qf0xb.raidsignupbot.infrastructure.mapper.jpa.GuildMemberEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaGuildMemberApplicationAdapter
    extends AbstractJpaApplicationAdapter<String, GuildMember, GuildMemberEntity, GuildMemberEntityMapper, GuildMemberEntityRepository>
    implements GuildMemberApplicationPort {
  @Inject
  GuildMemberEntityMapper mapper;

  @Inject
  GuildMemberEntityRepository repository;

  @Override
  protected GuildMemberEntityMapper getMapper() {
    return mapper;
  }

  @Override
  protected GuildMemberEntityRepository getRepository() {
    return repository;
  }

  @Override
  public Optional<GuildMember> findByUserId(String userId) {
    return getRepository().findByIdOptional(userId)
        .map(getMapper()::toDomain);
  }

  @Override
  public Set<GuildMember> findByGuildId(String guildId) {
    return getRepository()
        .find(
            """
            select distinct member
            from GuildMemberEntity member
            join member.activeGuilds guild
            where guild.id = ?1
            """,
            guildId)
        .list()
        .stream()
        .map(getMapper()::toDomain)
        .collect(Collectors.toSet());
  }

  @Override
  public GuildMember save(GuildMember guildMember) {
    return save(guildMember.getUserId(), guildMember);
  }

  @Override
  public GuildMemberEntity partialUpdate(GuildMember domain, GuildMemberEntity entity) {
    return getMapper().partialUpdate(domain, entity);
  }
}
