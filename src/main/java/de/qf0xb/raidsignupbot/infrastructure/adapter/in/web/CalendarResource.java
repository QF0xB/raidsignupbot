package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateService;
import de.qf0xb.raidsignupbot.domain.model.calendar.RaidCalendarEntry;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.calendar.RaidCalendarPublisher;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Web counterpart of /calendar, /calendarconfig, /scheduleevent, and /calendarremove. */
@Path("/servers/{serverId}/calendar")
@Authenticated
@Blocking
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class CalendarResource {
  private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("EEE dd.MM.yyyy", Locale.ENGLISH);

  @CheckedTemplate
  static class Templates {
    static native TemplateInstance calendar(
        String userName,
        WebAccessService.ServerAccess server,
        String calendarChannel,
        List<EntryView> entries,
        List<String> guilds,
        List<DiscordDirectory.Option> templates,
        List<DiscordDirectory.Option> members,
        List<DiscordDirectory.Option> textChannels,
        List<String> durations,
        String today,
        String message,
        String error);
  }

  public record EntryView(
      Long id, String day, String guildName, String template, String hosterName, String duration,
      Long eventId, boolean past, boolean canCreateEvent) {}

  private final CurrentUser currentUser;
  private final WebAccessService accessService;
  private final RaidCalendarService calendarService;
  private final RaidCalendarPublisher publisher;
  private final ServerService serverService;
  private final GuildApplicationPort guildPort;
  private final TemplateService templateService;
  private final DiscordDirectory directory;

  public CalendarResource(
      CurrentUser currentUser,
      WebAccessService accessService,
      RaidCalendarService calendarService,
      RaidCalendarPublisher publisher,
      ServerService serverService,
      GuildApplicationPort guildPort,
      TemplateService templateService,
      DiscordDirectory directory) {
    this.currentUser = currentUser;
    this.accessService = accessService;
    this.calendarService = calendarService;
    this.publisher = publisher;
    this.serverService = serverService;
    this.guildPort = guildPort;
    this.templateService = templateService;
    this.directory = directory;
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance show(
      @PathParam("serverId") String serverId,
      @QueryParam("message") String message,
      @QueryParam("error") String error) {
    String userId = currentUser.id();
    WebAccessService.ServerAccess access = accessService.serverAccess(serverId, userId);
    List<RaidCalendarEntry> entries = calendarService.findEntries(serverId);
    if (!access.any() && entries.stream().noneMatch(entry -> userId.equals(entry.getHosterUserId()))) {
      throw new ForbiddenException("You have no access to this server's calendar.");
    }

    Server server = serverService.findServerByIdOptional(serverId).orElse(new Server(serverId));
    List<EventTemplate> templates = templateService.findAllTemplates(serverId).stream()
        .sorted(Comparator.comparing(EventTemplate::getName, String.CASE_INSENSITIVE_ORDER))
        .toList();
    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    Instant now = Instant.now();

    List<EntryView> entryViews = entries.stream()
        .map(entry -> new EntryView(
            entry.getId(),
            DAY_FORMAT.format(entry.getStart().atZone(ZoneId.systemDefault())),
            entry.getGuildName(),
            templates.stream().filter(template -> template.getKey().equals(entry.getTemplateKey()))
                .map(EventTemplate::getName).findFirst().orElse(entry.getTemplateKey()),
            directory.memberName(serverId, entry.getHosterUserId()),
            entry.getDuration(),
            entry.getEventId(),
            entry.getEnd().isBefore(now),
            entry.getEventId() == null && !entry.getEnd().isBefore(now)
                && (access.manageGuilds() || userId.equals(entry.getHosterUserId()))))
        .toList();

    return Templates.calendar(
        currentUser.displayName(),
        access,
        server.getRaidCalendarChannelId() == null ? null : directory.channelName(server.getRaidCalendarChannelId()),
        entryViews,
        guildPort.findGuildByServerId(serverId).stream().map(Guild::getName).sorted().toList(),
        templates.stream().map(template -> new DiscordDirectory.Option(template.getKey(), template.getName())).toList(),
        access.manageGuilds() ? directory.members(serverId) : List.of(),
        access.manageGuilds() ? directory.textChannels(serverId) : List.of(),
        WebForms.DURATIONS,
        today.toString(),
        message,
        error);
  }

  @POST
  @Path("/entries")
  public Response schedule(
      @PathParam("serverId") String serverId,
      @FormParam("guildName") String guildName,
      @FormParam("templateKey") String templateKey,
      @FormParam("hosterUserId") String hosterUserId,
      @FormParam("date") String date,
      @FormParam("duration") String duration) {
    requireGuildManager(serverId);
    return WebForms.run(path(serverId), () -> {
      Instant start = WebForms.startOfDay(date);
      RaidCalendarEntry entry = calendarService.createEntry(
          serverId,
          WebForms.required(guildName, "guild"),
          WebForms.required(hosterUserId, "host"),
          WebForms.required(templateKey, "template"),
          start,
          start.plus(WebForms.duration(duration)));
      return "Added calendar entry " + entry.getId() + "." + publish(serverId);
    });
  }

  @POST
  @Path("/entries/{entryId}/delete")
  public Response remove(@PathParam("serverId") String serverId, @PathParam("entryId") Long entryId) {
    requireGuildManager(serverId);
    return WebForms.run(path(serverId), () -> {
      calendarService.deleteEntry(serverId, entryId);
      return "Removed calendar entry " + entryId + "." + publish(serverId);
    });
  }

  @POST
  @Path("/config")
  public Response configure(@PathParam("serverId") String serverId, @FormParam("channelId") String channelId) {
    requireGuildManager(serverId);
    return WebForms.run(path(serverId), () -> {
      serverService.updateCalendarSettings(serverId, WebForms.required(channelId, "calendar channel"));
      return "Calendar channel saved." + publish(serverId);
    });
  }

  @POST
  @Path("/refresh")
  public Response refresh(@PathParam("serverId") String serverId) {
    WebAccessService.ServerAccess access = accessService.serverAccess(serverId, currentUser.id());
    access.require(access.any());
    return WebForms.run(path(serverId), () -> {
      calendarService.requireCalendarConfigured(serverId);
      publisher.publish(serverId);
      return "Raid calendar refreshed in Discord.";
    });
  }

  private void requireGuildManager(String serverId) {
    WebAccessService.ServerAccess access = accessService.serverAccess(serverId, currentUser.id());
    access.require(access.manageGuilds());
  }

  private String publish(String serverId) {
    try {
      calendarService.requireCalendarConfigured(serverId);
      publisher.publish(serverId);
      return "";
    } catch (RuntimeException ex) {
      return " The Discord calendar message could not be refreshed: " + ex.getMessage();
    }
  }

  private static String path(String serverId) {
    return "/servers/" + serverId + "/calendar";
  }
}
