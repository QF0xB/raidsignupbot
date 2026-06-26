package de.qf0xb.raidsignupbot.application.port.guild;

import de.qf0xb.raidsignupbot.domain.model.guild.Guild;

import java.util.Optional;
import java.util.Set;

public interface GuildApplicationPort {
  Optional<Guild> findById(Long guildId);
  Set<Guild> findGuildByServerId(String serverId);
  Optional<Guild> findByServerIdAndName(String serverId, String name);
  Optional<Guild> findByName(String name);
  Guild save(Guild guild);
}
