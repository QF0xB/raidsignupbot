package de.qf0xb.raidsignupbot.application.service.server;

import de.qf0xb.raidsignupbot.application.port.server.ServerApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class ServerService {
  private final ServerApplicationPort serverApplicationPort;

  public ServerService(ServerApplicationPort serverApplicationPort) {
    this.serverApplicationPort = serverApplicationPort;
  }

  @Transactional
  public Server ensureServer(String serverId) {
    return serverApplicationPort.findByServerId(serverId)
        .orElseGet(() -> serverApplicationPort.save(new Server(serverId)));
  }

  @Transactional
  public Optional<Server> findServerByIdOptional(String serverId) {
    return serverApplicationPort.findByServerId(serverId);
  }

  @Transactional
  public Server findServerById(String serverId) {
    return serverApplicationPort.findByServerId(serverId)
        .orElseThrow(() -> new IllegalArgumentException("Server with id " + serverId + " not found"));
  }

  @Transactional
  public Server updatePermissions(
      String serverId,
      String guildManagerRoleId,
      String templateManagerRoleId,
      String eventManagerRoleId) {
    return updateSettings(serverId, guildManagerRoleId, templateManagerRoleId, eventManagerRoleId, null, null, null);
  }

  @Transactional
  public Server updateSettings(
      String serverId,
      String guildManagerRoleId,
      String templateManagerRoleId,
      String eventManagerRoleId,
      Boolean manageSignupChannels,
      Boolean manageVoiceChannels,
      String raidVoiceChannelCategory) {
    Server server = ensureServer(serverId);

    if (guildManagerRoleId != null) {
      server.setGuildManagerRoleId(guildManagerRoleId);
    }
    if (templateManagerRoleId != null) {
      server.setTemplateManagerRoleId(templateManagerRoleId);
    }
    if (eventManagerRoleId != null) {
      server.setEventManagerRoleId(eventManagerRoleId);
    }
    if (manageSignupChannels != null) {
      server.setManageSignupChannels(manageSignupChannels);
    }
    if (manageVoiceChannels != null) {
      server.setManageVoiceChannels(manageVoiceChannels);
    }
    if (raidVoiceChannelCategory != null) {
      server.setRaidVoiceChannelCategory(raidVoiceChannelCategory);
    }

    return serverApplicationPort.save(server);
  }
}
