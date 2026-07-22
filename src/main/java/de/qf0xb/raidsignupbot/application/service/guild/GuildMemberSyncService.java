package de.qf0xb.raidsignupbot.application.service.guild;

import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.Set;

@Slf4j
@ApplicationScoped
public class GuildMemberSyncService {
  @Inject
  JDA api;

  @Inject
  GuildApplicationPort guildApplicationPort;

  @Inject
  GuildMemberService guildMemberService;

  @Transactional
  public int syncGuildMembers(Guild guild) {
    if (!guild.isRaiderRolesEnabled() || guild.getRaiderRoleId() == null || guild.getRaiderRoleId().isBlank()) {
      return 0;
    }

    net.dv8tion.jda.api.entities.Guild discordGuild = api.getGuildById(guild.getServerId());
    if (discordGuild == null) {
      log.warn("Skipping guild member sync for {} because Discord guild {} is unavailable", guild.getName(), guild.getServerId());
      return 0;
    }

    Role role = discordGuild.getRoleById(guild.getRaiderRoleId());
    if (role == null) {
      log.warn("Skipping guild member sync for {} because role {} is missing", guild.getName(), guild.getRaiderRoleId());
      return 0;
    }

    var members = discordGuild.getMembersWithRoles(role);
    int syncedMembers = 0;
    for (Member member : members) {
      guildMemberService.ensureGuildMember(member.getId(), guild.getServerId(), guild.getName());
      syncedMembers++;
    }

    log.info(
        "Synced {} members for guild {} from role {} on server {}",
        syncedMembers,
        guild.getName(),
        guild.getRaiderRoleId(),
        guild.getServerId());
    return syncedMembers;
  }

  @Transactional
  public int syncGuildMembersByServerId(String serverId) {
    int total = 0;
    Set<Guild> guilds = guildApplicationPort.findGuildByServerId(serverId);
    for (Guild guild : guilds) {
      total += syncGuildMembers(guild);
    }
    return total;
  }

  @Transactional
  public int syncMemberToMatchingGuilds(String serverId, String userId, String roleId) {
    int synced = 0;
    Set<Guild> guilds = guildApplicationPort.findGuildByServerId(serverId);
    for (Guild guild : guilds) {
      if (guild.isRaiderRolesEnabled()
          && roleId != null
          && roleId.equals(guild.getRaiderRoleId())) {
        guildMemberService.ensureGuildMember(userId, guild.getServerId(), guild.getName());
        synced++;
      }
    }
    return synced;
  }

  @Transactional
  public int syncAllKnownGuilds() {
    int total = 0;
    for (net.dv8tion.jda.api.entities.Guild discordGuild : api.getGuilds()) {
      total += syncGuildMembersByServerId(discordGuild.getId());
    }
    return total;
  }
}
