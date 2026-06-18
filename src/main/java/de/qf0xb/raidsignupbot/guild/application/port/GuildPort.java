package de.qf0xb.raidsignupbot.guild.application.port;

import de.qf0xb.raidsignupbot.guild.domain.model.Guild;

import java.util.Optional;

public interface GuildPort {
  Optional<Guild> findByGuildId(String guildId);
  Guild save(Guild guild);
}
