package de.qf0xb.raidsignupbot.application.service.guild;

import de.qf0xb.raidsignupbot.application.dto.CreateGuildCommand;
import de.qf0xb.raidsignupbot.application.dto.UpdateGuildCommand;
import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GuildService {
  private final GuildApplicationPort guildApplicationPort;
  private final ServerService serverService;

  public GuildService(GuildApplicationPort guildApplicationPort, ServerService serverService) {
    this.guildApplicationPort = guildApplicationPort;
    this.serverService = serverService;
  }

  public Guild createGuild(CreateGuildCommand command) {
    Guild guild = new Guild(command.getName(), command.getServerId());
    guild.setServer(serverService.ensureServer(command.getServerId()));
    guild.setBotAdmin(command.isAdminBot());
    guild.setRaiderRolesEnabled(command.isRaiderRolesEnabled());
    guild.setEventTemplatesEnabled(command.isEventTemplatesEnabled());
    guild.setEventRoleId(command.getEventRoleId());

    if (command.isRaiderRolesEnabled()) {
      guild.setRaiderRoleId(command.getRaiderRoleId());
    }
    return guildApplicationPort.save(guild);
  }

  public Guild updateGuild(String guildName, UpdateGuildCommand command) {
    Guild guild = findGuildByName(guildName);
    guild.setName(command.getName());
    guild.setServerId(command.getServerId());
    guild.setServer(serverService.ensureServer(command.getServerId()));
    guild.setBotAdmin(command.isAdminBot());
    guild.setRaiderRolesEnabled(command.isRaiderRolesEnabled());
    guild.setRaiderRoleId(command.getRaiderRoleId());
    guild.setEventTemplatesEnabled(command.isEventTemplatesEnabled());
    guild.setEventRoleId(command.getEventRoleId());
    return guildApplicationPort.save(guild);
  }

  public Guild updateGuildSettings(
      String guildName,
      String name,
      Boolean adminBot,
      Boolean raiderRolesEnabled,
      String raiderRoleId,
      Boolean eventTemplatesEnabled,
      String eventRoleId) {
    Guild guild = findGuildByName(guildName);

    if (name != null) {
      guild.setName(name);
    }
    if (adminBot != null) {
      guild.setBotAdmin(adminBot);
    }
    if (raiderRolesEnabled != null) {
      guild.setRaiderRolesEnabled(raiderRolesEnabled);
      if (!raiderRolesEnabled) {
        guild.setRaiderRoleId(null);
      }
    }
    if (raiderRoleId != null) {
      guild.setRaiderRoleId(raiderRoleId);
    }
    if (eventTemplatesEnabled != null) {
      guild.setEventTemplatesEnabled(eventTemplatesEnabled);
    }
    if (eventRoleId != null) {
      guild.setEventRoleId(eventRoleId);
    }

    return guildApplicationPort.save(guild);
  }

  public Guild setBotAdminOfGuild(String guildName, boolean admin) {
    Guild guild = findGuildByName(guildName);
    guild.setBotAdmin(admin);
    return guildApplicationPort.save(guild);
  }

  public Guild setRaiderRolesEnabledOfGuild(String guildName, boolean enabled, String raiderRoleId) {
    Guild guild = findGuildByName(guildName);
    guild.setRaiderRolesEnabled(enabled);
    if (enabled) {
      guild.setRaiderRoleId(raiderRoleId);
    }
    return guildApplicationPort.save(guild);
  }

  public Guild setEventTemplatesEnabledOfGuild(String guildName, boolean enabled) {
    Guild guild = findGuildByName(guildName);
    guild.setEventTemplatesEnabled(enabled);
    return guildApplicationPort.save(guild);
  }

  public Guild findGuildById(Long id) {
    return guildApplicationPort.findById(id).orElseThrow(
        () -> new IllegalArgumentException("Guild with id " + id + " not found")
    );
  }

  public Guild findGuildByName(String name) {
    return guildApplicationPort.findByName(name).orElseThrow(
        () -> new IllegalArgumentException("Guild with name " + name + " not found")
    );
  }

  public Guild findGuildByServerIdAndName(String serverId, String name) {
    return guildApplicationPort.findByServerIdAndName(serverId, name).orElseThrow(
        () -> new IllegalArgumentException("Guild with name " + name + " not found for server id " + serverId)
    );
  }

  public Guild findGuildByServerId(String serverId) {
    return guildApplicationPort.findGuildByServerId(serverId).stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Guild with server id " + serverId + " not found"));
  }
}
