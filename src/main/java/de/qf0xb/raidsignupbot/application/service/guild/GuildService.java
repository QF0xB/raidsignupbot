package de.qf0xb.raidsignupbot.application.service.guild;

import de.qf0xb.raidsignupbot.application.dto.CreateGuildCommand;
import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GuildService {
  private final GuildApplicationPort guildApplicationPort;

  public GuildService(GuildApplicationPort guildApplicationPort) {
    this.guildApplicationPort = guildApplicationPort;
  }

  public Guild createGuild(CreateGuildCommand command) {
    Guild guild = new Guild(command.getGuildId(), command.getName());
    guild.setBotAdmin(command.isAdminBot());
    guild.setRaiderRolesEnabled(command.isRaiderRolesEnabled());
    guild.setEventTemplatesEnabled(command.isEventTemplatesEnabled());

    if (command.isRaiderRolesEnabled()) {
      guild.setRaiderRoleId(command.getRaiderRoleId());
    }
    return guildApplicationPort.save(guild);
  }

  public Guild setBotAdminOfGuild(String guildId, boolean admin) {
    Guild guild = findGuildById(guildId);
    guild.setBotAdmin(admin);
    return guildApplicationPort.save(guild);
  }

  public Guild setRaiderRolesEnabledOfGuild(String guildId, boolean enabled, String raiderRoleId) {
    Guild guild = findGuildById(guildId);
    guild.setRaiderRolesEnabled(enabled);
    if (enabled) {
      guild.setRaiderRoleId(raiderRoleId);
    }
    return guildApplicationPort.save(guild);
  }

  public Guild setEventTemplatesEnabledOfGuild(String guildId, boolean enabled) {
    Guild guild = findGuildById(guildId);
    guild.setEventTemplatesEnabled(enabled);
    return guildApplicationPort.save(guild);
  }

  public Guild findGuildById(String guildId) {
    return guildApplicationPort.findById(guildId).orElseThrow(
        () -> new IllegalArgumentException("Guild with id " + guildId + " not found")
    );
  }
}
