package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command;

import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.entities.Role;

@ApplicationScoped
public class CommandPermissionService {
  @Inject
  ServerService serverService;

  public boolean isAllowed(SlashCommandInteractionEvent event, CommandAccessLevel accessLevel) {
    if (accessLevel == CommandAccessLevel.PUBLIC) {
      return true;
    }

    if (event.getGuild() == null) {
      return false;
    }

    return isMemberAllowed(event.getMember(), accessLevel);
  }

  /** Shared by slash commands and the web UI so both grant the same access. */
  public boolean isMemberAllowed(Member member, CommandAccessLevel accessLevel) {
    if (accessLevel == CommandAccessLevel.PUBLIC) {
      return true;
    }

    if (member == null) {
      return false;
    }

    if (member.hasPermission(Permission.ADMINISTRATOR)
        || member.hasPermission(Permission.MANAGE_SERVER)
        || member.isOwner()) {
      return true;
    }

    Server server = serverService.findServerByIdOptional(member.getGuild().getId()).orElse(null);
    if (server == null) {
      return false;
    }

    return switch (accessLevel) {
      case MANAGE_GUILDS -> hasRole(member, server.getGuildManagerRoleId());
      case MANAGE_TEMPLATES -> hasRole(member, server.getTemplateManagerRoleId());
      case MANAGE_EVENTS -> hasRole(member, server.getEventManagerRoleId());
      case ADMIN -> false;
      case PUBLIC -> true;
    };
  }

  private boolean hasRole(Member member, String roleId) {
    if (roleId == null || roleId.isBlank()) {
      return false;
    }

    return member.getRoles().stream()
        .map(Role::getId)
        .anyMatch(roleId::equals);
  }
}
