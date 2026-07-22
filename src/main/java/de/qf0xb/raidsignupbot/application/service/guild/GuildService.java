package de.qf0xb.raidsignupbot.application.service.guild;

import de.qf0xb.raidsignupbot.application.dto.CreateGuildCommand;
import de.qf0xb.raidsignupbot.application.dto.UpdateGuildCommand;
import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildMemberSyncService;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GuildService {
  private final GuildApplicationPort guildApplicationPort;
  private final ServerService serverService;
  private final GuildMemberSyncService guildMemberSyncService;

  public GuildService(GuildApplicationPort guildApplicationPort, ServerService serverService, GuildMemberSyncService guildMemberSyncService) {
    this.guildApplicationPort = guildApplicationPort;
    this.serverService = serverService;
    this.guildMemberSyncService = guildMemberSyncService;
  }

  public Guild createGuild(CreateGuildCommand command) {
    Guild guild = new Guild(command.getName(), command.getServerId());
    guild.setServer(serverService.ensureServer(command.getServerId()));
    guild.setRaiderRolesEnabled(command.isRaiderRolesEnabled());
    guild.setEventRoleId(command.getEventRoleId());

    if (command.isRaiderRolesEnabled()) {
      guild.setRaiderRoleId(command.getRaiderRoleId());
    }
    Guild savedGuild = guildApplicationPort.save(guild);
    guildMemberSyncService.syncGuildMembers(savedGuild);
    return savedGuild;
  }

  public Guild updateGuild(String guildName, UpdateGuildCommand command) {
    Guild guild = findGuildByName(guildName);
    guild.setName(command.getName());
    guild.setServerId(command.getServerId());
    guild.setServer(serverService.ensureServer(command.getServerId()));
    guild.setRaiderRolesEnabled(command.isRaiderRolesEnabled());
    guild.setRaiderRoleId(command.getRaiderRoleId());
    guild.setEventRoleId(command.getEventRoleId());
    Guild savedGuild = guildApplicationPort.save(guild);
    guildMemberSyncService.syncGuildMembers(savedGuild);
    return savedGuild;
  }

  public Guild updateGuildSettings(
      String guildName,
      String name,
      Boolean raiderRolesEnabled,
      String raiderRoleId,
      String eventRoleId) {
    Guild guild = findGuildByName(guildName);

    if (name != null) {
      guild.setName(name);
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
    if (eventRoleId != null) {
      guild.setEventRoleId(eventRoleId);
    }

    Guild savedGuild = guildApplicationPort.save(guild);
    guildMemberSyncService.syncGuildMembers(savedGuild);
    return savedGuild;
  }

  public Guild setRaiderRolesEnabledOfGuild(String guildName, boolean enabled, String raiderRoleId) {
    Guild guild = findGuildByName(guildName);
    guild.setRaiderRolesEnabled(enabled);
    if (enabled) {
      guild.setRaiderRoleId(raiderRoleId);
    }
    Guild savedGuild = guildApplicationPort.save(guild);
    guildMemberSyncService.syncGuildMembers(savedGuild);
    return savedGuild;
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
