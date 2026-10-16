package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import de.qf0xb.raidsignupbot.application.dto.CreateGuildCommand;
import de.qf0xb.raidsignupbot.application.dto.UpdateGuildCommand;
import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.application.service.guild.GuildMemberSyncService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Comparator;
import java.util.List;

/** Web counterpart of /onboard, /editguild, and /syncguildmembers. */
@Path("/servers/{serverId}/settings")
@Authenticated
@Blocking
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class SettingsResource {
  @CheckedTemplate
  static class Templates {
    static native TemplateInstance settings(
        String userName,
        WebAccessService.ServerAccess server,
        Server settings,
        List<Guild> guilds,
        List<DiscordDirectory.Option> roles,
        List<DiscordDirectory.Option> categories,
        String message,
        String error);
  }

  private final CurrentUser currentUser;
  private final WebAccessService accessService;
  private final ServerService serverService;
  private final GuildService guildService;
  private final GuildApplicationPort guildPort;
  private final GuildMemberSyncService syncService;
  private final DiscordDirectory directory;

  public SettingsResource(
      CurrentUser currentUser,
      WebAccessService accessService,
      ServerService serverService,
      GuildService guildService,
      GuildApplicationPort guildPort,
      GuildMemberSyncService syncService,
      DiscordDirectory directory) {
    this.currentUser = currentUser;
    this.accessService = accessService;
    this.serverService = serverService;
    this.guildService = guildService;
    this.guildPort = guildPort;
    this.syncService = syncService;
    this.directory = directory;
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance show(
      @PathParam("serverId") String serverId,
      @QueryParam("message") String message,
      @QueryParam("error") String error) {
    WebAccessService.ServerAccess access = requireGuildManager(serverId);
    return Templates.settings(
        currentUser.displayName(),
        access,
        serverService.findServerByIdOptional(serverId).orElse(new Server(serverId)),
        guildPort.findGuildByServerId(serverId).stream()
            .sorted(Comparator.comparing(Guild::getName, String.CASE_INSENSITIVE_ORDER)).toList(),
        directory.roles(serverId),
        directory.categories(serverId),
        message,
        error);
  }

  @POST
  @Path("/server")
  public Response updateServer(
      @PathParam("serverId") String serverId,
      @FormParam("guildManagerRoleId") String guildManagerRoleId,
      @FormParam("templateManagerRoleId") String templateManagerRoleId,
      @FormParam("eventManagerRoleId") String eventManagerRoleId,
      @FormParam("manageSignupChannels") String manageSignupChannels,
      @FormParam("manageVoiceChannels") String manageVoiceChannels,
      @FormParam("raidVoiceChannelCategory") String raidVoiceChannelCategory) {
    requireGuildManager(serverId);
    return WebForms.run(path(serverId), () -> {
      serverService.updateSettings(
          serverId,
          WebForms.blankToNull(guildManagerRoleId),
          WebForms.blankToNull(templateManagerRoleId),
          WebForms.blankToNull(eventManagerRoleId),
          "true".equals(manageSignupChannels),
          "true".equals(manageVoiceChannels),
          WebForms.blankToNull(raidVoiceChannelCategory));
      return "Server settings saved.";
    });
  }

  @POST
  @Path("/guilds")
  public Response createGuild(
      @PathParam("serverId") String serverId,
      @FormParam("name") String name,
      @FormParam("raiderRolesEnabled") String raiderRolesEnabled,
      @FormParam("raiderRoleId") String raiderRoleId,
      @FormParam("eventRoleId") String eventRoleId) {
    requireGuildManager(serverId);
    return WebForms.run(path(serverId), () -> {
      boolean raiderRoles = "true".equals(raiderRolesEnabled);
      Guild guild = guildService.createGuild(CreateGuildCommand.builder()
          .name(WebForms.required(name, "guild name"))
          .serverId(serverId)
          .raiderRolesEnabled(raiderRoles)
          .raiderRoleId(raiderRoles ? WebForms.blankToNull(raiderRoleId) : null)
          .eventRoleId(WebForms.blankToNull(eventRoleId))
          .build());
      return "Guild " + guild.getName() + " created and its members synced.";
    });
  }

  @POST
  @Path("/guilds/{guildName}")
  public Response updateGuild(
      @PathParam("serverId") String serverId,
      @PathParam("guildName") String guildName,
      @FormParam("name") String name,
      @FormParam("raiderRolesEnabled") String raiderRolesEnabled,
      @FormParam("raiderRoleId") String raiderRoleId,
      @FormParam("eventRoleId") String eventRoleId) {
    requireGuildManager(serverId);
    return WebForms.run(path(serverId), () -> {
      boolean raiderRoles = "true".equals(raiderRolesEnabled);
      Guild guild = guildService.updateGuild(guildName, UpdateGuildCommand.builder()
          .name(WebForms.required(name, "guild name"))
          .serverId(serverId)
          .raiderRolesEnabled(raiderRoles)
          .raiderRoleId(raiderRoles ? WebForms.blankToNull(raiderRoleId) : null)
          .eventRoleId(WebForms.blankToNull(eventRoleId))
          .build());
      return "Guild " + guild.getName() + " saved and its members synced.";
    });
  }

  @POST
  @Path("/sync")
  public Response syncMembers(@PathParam("serverId") String serverId) {
    requireGuildManager(serverId);
    return WebForms.run(path(serverId),
        () -> "Synced " + syncService.syncGuildMembersByServerId(serverId) + " guild member(s).");
  }

  private WebAccessService.ServerAccess requireGuildManager(String serverId) {
    WebAccessService.ServerAccess access = accessService.serverAccess(serverId, currentUser.id());
    access.require(access.manageGuilds());
    return access;
  }

  private static String path(String serverId) {
    return "/servers/" + serverId + "/settings";
  }
}
