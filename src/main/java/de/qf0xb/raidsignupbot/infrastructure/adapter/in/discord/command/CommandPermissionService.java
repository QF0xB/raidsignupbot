package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command;

import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.entities.Role;

import java.util.Objects;

@ApplicationScoped
public class CommandPermissionService {
  @Inject
  ServerService serverService;

  public boolean isAllowed(SlashCommandInteractionEvent event, CommandAccessLevel accessLevel) {
    if (accessLevel == CommandAccessLevel.PUBLIC) {
      return true;
    }

    if (event.getGuild() == null || event.getMember() == null) {
      return false;
    }

    if (event.getMember().hasPermission(Permission.ADMINISTRATOR)
        || event.getMember().hasPermission(Permission.MANAGE_SERVER)
        || isServerOwner(event)) {
      return true;
    }

    Server server = serverService.findServerByIdOptional(event.getGuild().getId()).orElse(null);
    if (server == null) {
      return false;
    }

    return switch (accessLevel) {
      case MANAGE_GUILDS -> hasRole(event, server.getGuildManagerRoleId());
      case MANAGE_TEMPLATES -> hasRole(event, server.getTemplateManagerRoleId());
      case MANAGE_EVENTS -> hasRole(event, server.getEventManagerRoleId());
      case PUBLIC -> true;
    };
  }

  private boolean hasRole(SlashCommandInteractionEvent event, String roleId) {
    if (roleId == null || roleId.isBlank()) {
      return false;
    }

    return event.getMember().getRoles().stream()
        .map(Role::getId)
        .anyMatch(roleId::equals);
  }

  private boolean isServerOwner(SlashCommandInteractionEvent event) {
    return Objects.equals(String.valueOf(event.getGuild().getOwnerIdLong()), event.getUser().getId());
  }
}
