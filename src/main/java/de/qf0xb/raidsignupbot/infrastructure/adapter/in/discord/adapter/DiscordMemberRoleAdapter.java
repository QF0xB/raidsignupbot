package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.adapter;

import de.qf0xb.raidsignupbot.application.port.guild.MemberRolePort;
import jakarta.enterprise.context.ApplicationScoped;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;

/** Needs the Manage Roles permission, and the bot's own role must be above the raider role. */
@ApplicationScoped
public class DiscordMemberRoleAdapter implements MemberRolePort {
  private final JDA api;

  public DiscordMemberRoleAdapter(JDA api) {
    this.api = api;
  }

  @Override
  public boolean hasRole(String serverId, String userId, String roleId) {
    Guild server = server(serverId);
    Member member = server.getMemberById(userId);
    if (member == null) {
      member = server.retrieveMemberById(userId).complete();
    }
    return member.getRoles().stream().anyMatch(role -> role.getId().equals(roleId));
  }

  @Override
  public void addRole(String serverId, String userId, String roleId) {
    Guild server = server(serverId);
    server.addRoleToMember(UserSnowflake.fromId(userId), role(server, roleId)).complete();
  }

  @Override
  public void removeRole(String serverId, String userId, String roleId) {
    Guild server = server(serverId);
    server.removeRoleFromMember(UserSnowflake.fromId(userId), role(server, roleId)).complete();
  }

  private Guild server(String serverId) {
    Guild server = api.getGuildById(serverId);
    if (server == null) {
      throw new IllegalStateException("The bot is not on server " + serverId);
    }
    return server;
  }

  private static Role role(Guild server, String roleId) {
    Role role = server.getRoleById(roleId);
    if (role == null) {
      throw new IllegalStateException("Role " + roleId + " does not exist on server " + server.getId());
    }
    return role;
  }
}
