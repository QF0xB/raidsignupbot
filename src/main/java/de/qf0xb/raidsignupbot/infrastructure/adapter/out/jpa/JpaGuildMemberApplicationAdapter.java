package de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa;

import de.qf0xb.raidsignupbot.application.event.port.GuildMemberApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.infrastructure.jpa.GuildMemberEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.GuildMemberEntityRepository;
import de.qf0xb.raidsignupbot.infrastructure.mapper.jpa.GuildMemberEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaGuildMemberApplicationAdapter
    extends AbstractJpaApplicationAdapter<String, GuildMember, GuildMemberEntity, GuildMemberEntityMapper, GuildMemberEntityRepository>
    implements GuildMemberApplicationPort {

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
            where guild.guildId = ?1
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
}
