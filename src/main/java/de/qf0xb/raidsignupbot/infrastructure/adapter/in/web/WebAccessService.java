package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandPermissionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ForbiddenException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;


/** Grants web access with the same rules as the corresponding slash commands. */
@ApplicationScoped
public class WebAccessService {
  private final JDA api;
  private final CommandPermissionService permissionService;

  public WebAccessService(JDA api, CommandPermissionService permissionService) {
    this.api = api;
    this.permissionService = permissionService;
  }

  public boolean isAllowed(String serverId, String userId, CommandAccessLevel accessLevel) {
    Guild server = api.getGuildById(serverId);
    return server != null && isAllowed(server, userId, accessLevel);
  }

  public void require(String serverId, String userId, CommandAccessLevel accessLevel) {
    if (!isAllowed(serverId, userId, accessLevel)) {
      throw new ForbiddenException("You are not allowed to do this on this server.");
    }
  }

  public ServerAccess serverAccess(String serverId, String userId) {
    Guild server = api.getGuildById(serverId);
    if (server == null) {
      throw new jakarta.ws.rs.NotFoundException("The bot is not on this server.");
    }
    return serverAccess(server, userId);
  }

  private ServerAccess serverAccess(Guild server, String userId) {
    return new ServerAccess(
        server.getId(),
        server.getName(),
        isAllowed(server, userId, CommandAccessLevel.MANAGE_EVENTS),
        isAllowed(server, userId, CommandAccessLevel.MANAGE_GUILDS),
        isAllowed(server, userId, CommandAccessLevel.MANAGE_TEMPLATES),
        isAllowed(server, userId, CommandAccessLevel.ADMIN));
  }

  public EventPermissions eventPermissions(Event event, String userId) {
    String serverId = event.getGuild().getServerId();
    boolean eventAdmin = event.getEventAdmin() != null && userId.equals(event.getEventAdmin().getUserId());
    return new EventPermissions(
        isAllowed(serverId, userId, CommandAccessLevel.MANAGE_EVENTS),
        isAllowed(serverId, userId, CommandAccessLevel.ADMIN),
        eventAdmin);
  }

  private boolean isAllowed(Guild server, String userId, CommandAccessLevel accessLevel) {
    return permissionService.isMemberAllowed(server.getMemberById(userId), accessLevel);
  }

  public record ServerAccess(
      String id, String name, boolean manageEvents, boolean manageGuilds, boolean manageTemplates, boolean admin) {
    public boolean any() {
      return manageEvents || manageGuilds || manageTemplates || admin;
    }

    public void require(boolean allowed) {
      if (!allowed) {
        throw new ForbiddenException("You are not allowed to do this on this server.");
      }
    }
  }

  /**
   * What the user may do with one event. Like the Discord approval buttons, only the event admin
   * decides on a pending proposal.
   */
  public record EventPermissions(boolean manage, boolean admin, boolean eventAdmin) {
    public boolean view() {
      return manage || eventAdmin;
    }

    /** A pending proposal is edited by the event admin, published teams by event managers. */
    public boolean canSwap(boolean approvalPending) {
      return approvalPending ? eventAdmin : manage;
    }
  }
}
