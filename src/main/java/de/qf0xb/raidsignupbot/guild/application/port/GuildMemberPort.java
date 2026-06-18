package de.qf0xb.raidsignupbot.guild.application.port;

import de.qf0xb.raidsignupbot.guild.domain.model.Guild;
import de.qf0xb.raidsignupbot.guild.domain.model.GuildMember;

import java.util.Optional;

public interface GuildMemberPort {
  Optional<GuildMember> findByUserIdAndGuild(String userId, Guild guild);
  GuildMember save(GuildMember member);
}
