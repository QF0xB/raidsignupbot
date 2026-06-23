package de.qf0xb.raidsignupbot.application.port.guild;

import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;

import java.util.Optional;
import java.util.Set;

public interface GuildMemberApplicationPort {
  Optional<GuildMember> findByUserId(String userId);
  Set<GuildMember> findByGuildId(String guildId);
  GuildMember save(GuildMember guildMember);
}
