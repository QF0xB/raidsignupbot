package de.qf0xb.raidsignupbot.application.service.guild;

import de.qf0xb.raidsignupbot.application.port.guild.GuildMemberApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Set;

@ApplicationScoped
public class GuildMemberService {
  private final GuildService guildService;
  private final GuildMemberApplicationPort guildMemberApplicationPort;

  public GuildMemberService(GuildService guildService, GuildMemberApplicationPort guildMemberApplicationPort) {
    this.guildService = guildService;
    this.guildMemberApplicationPort = guildMemberApplicationPort;
  }

  public GuildMember createGuildMember(String userId, String guildName) {
    Guild guild = guildService.findGuildByName(guildName);
    return createGuildMember(userId, guild.getServerId(), guild.getName());
  }

  public GuildMember createGuildMember(String userId, String serverId, String guildName) {
    GuildMember guildMember = new GuildMember(userId);
    guildMember.addActiveGuild(guildService.findGuildByServerIdAndName(serverId, guildName));
    return guildMemberApplicationPort.save(guildMember);
  }

  public GuildMember ensureGuildMember(String userId, String guildName) {
    Guild guild = guildService.findGuildByName(guildName);
    return ensureGuildMember(userId, guild.getServerId(), guild.getName());
  }

  public GuildMember ensureGuildMember(String userId, String serverId, String guildName) {
    Guild guild = guildService.findGuildByServerIdAndName(serverId, guildName);
    return guildMemberApplicationPort.findByUserId(userId)
        .map(guildMember -> {
          guildMember.addActiveGuild(guild);
          return guildMemberApplicationPort.save(guildMember);
        })
        .orElseGet(() -> createGuildMember(userId, guild.getServerId(), guild.getName()));
  }

  public GuildMember joinGuild(String userId, String guildName) {
    Guild guild = guildService.findGuildByName(guildName);
    GuildMember guildMember = findGuildMemberById(userId);
    guildMember.addActiveGuild(guild);
    return guildMemberApplicationPort.save(guildMember);
  }

  public GuildMember leaveGuild(String userId, String guildName) {
    Guild guild = guildService.findGuildByName(guildName);
    GuildMember guildMember = findGuildMemberById(userId);
    guildMember.removeActiveGuild(guild);
    return guildMemberApplicationPort.save(guildMember);
  }

  public GuildMember couldNotFindRole(String userId) {
    GuildMember guildMember = findGuildMemberById(userId);
    guildMember.increasePriority();
    return guildMemberApplicationPort.save(guildMember);
  }

  public GuildMember foundRole(String userId) {
    GuildMember guildMember = findGuildMemberById(userId);
    guildMember.assignedRole();
    return guildMemberApplicationPort.save(guildMember);
  }

  public GuildMember findGuildMemberById(String userId) {
    return guildMemberApplicationPort.findByUserId(userId)
        .orElseThrow(() -> new IllegalArgumentException("GuildMember with id " + userId + " not found"));
  }

  public Set<GuildMember> findGuildMembersByGuildId(String guildId) {
    return guildMemberApplicationPort.findByGuildId(guildId);
  }
}
