package de.qf0xb.raidsignupbot.application.event.port;

import de.qf0xb.raidsignupbot.domain.model.guild.Guild;

import java.util.Optional;

public interface GuildApplicationPort {
  Optional<Guild> findById(String guildId);
  Guild save(Guild guild);
}
