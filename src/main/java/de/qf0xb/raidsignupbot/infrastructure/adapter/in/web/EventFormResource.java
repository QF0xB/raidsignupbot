package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import de.qf0xb.raidsignupbot.application.dto.CreateEventCommand;
import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateService;
import de.qf0xb.raidsignupbot.domain.model.calendar.RaidCalendarEntry;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Web counterpart of /createevent and /editevent. Creation starts from a free calendar entry, which
 * fixes guild, host, template, day, and duration, exactly like the Discord wizard.
 */
@Path("/")
@Authenticated
@Blocking
public class EventFormResource {
  private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("EEE dd.MM.yyyy", Locale.ENGLISH);

  @CheckedTemplate
  static class Templates {
    static native TemplateInstance eventForm(String userName, EventForm form, String error);
  }

  /** Everything the form shows; {@code eventId} is null while creating. */
  public record EventForm(
      String serverId,
      String serverName,
      Long eventId,
      String title,
      List<DiscordDirectory.Option> entries,
      Long entryId,
      List<DiscordDirectory.Option> templates,
      String templateKey,
      List<String> guilds,
      String guildName,
      List<DiscordDirectory.Option> members,
      String hosterUserId,
      String eventAdminUserId,
      String start,
      List<String> durations,
      String duration,
      boolean askCategory,
      boolean askSignupChannel,
      boolean askVoiceChannel,
      List<DiscordDirectory.Option> categories,
      List<DiscordDirectory.Option> textChannels,
      List<DiscordDirectory.Option> voiceChannels,
      String categoryId,
      String signUpChannelId,
      String voiceChannelId,
      int donation,
      boolean donationPerWin,
      boolean teamsRequireApproval) {
    public boolean creating() {
      return eventId == null;
    }

    public boolean isEntry(String id) {
      return entryId != null && entryId.toString().equals(id);
    }
  }

  /** The submitted form fields. */
  public static class Fields {
    @FormParam("entryId") public Long entryId;
    @FormParam("templateKey") public String templateKey;
    @FormParam("guildName") public String guildName;
    @FormParam("hosterUserId") public String hosterUserId;
    @FormParam("eventAdminUserId") public String eventAdminUserId;
    @FormParam("start") public String start;
    @FormParam("duration") public String duration;
    @FormParam("categoryId") public String categoryId;
    @FormParam("signUpChannelId") public String signUpChannelId;
    @FormParam("voiceChannelId") public String voiceChannelId;
    @FormParam("donation") public String donation;
    @FormParam("donationType") public String donationType;
    @FormParam("teamsRequireApproval") public String teamsRequireApproval;
  }

  private final CurrentUser currentUser;
  private final WebAccessService accessService;
  private final EventService eventService;
  private final RaidCalendarService calendarService;
  private final ServerService serverService;
  private final TemplateService templateService;
  private final GuildApplicationPort guildPort;
  private final DiscordDirectory directory;

  public EventFormResource(
      CurrentUser currentUser,
      WebAccessService accessService,
      EventService eventService,
      RaidCalendarService calendarService,
      ServerService serverService,
      TemplateService templateService,
      GuildApplicationPort guildPort,
      DiscordDirectory directory) {
    this.currentUser = currentUser;
    this.accessService = accessService;
    this.eventService = eventService;
    this.calendarService = calendarService;
    this.serverService = serverService;
    this.templateService = templateService;
    this.guildPort = guildPort;
    this.directory = directory;
  }

  @GET
  @Path("servers/{serverId}/events/new")
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance newEvent(
      @PathParam("serverId") String serverId,
      @QueryParam("entryId") Long entryId,
      @QueryParam("error") String error) {
    WebAccessService.ServerAccess access = accessService.serverAccess(serverId, currentUser.id());
    List<RaidCalendarEntry> entries = creatableEntries(serverId, access);
    if (entries.isEmpty()) {
      throw new NotFoundException("There are no calendar entries you can create an event for.");
    }
    RaidCalendarEntry entry = entries.stream().filter(candidate -> candidate.getId().equals(entryId))
        .findFirst().orElse(entries.getFirst());

    return Templates.eventForm(currentUser.displayName(), form(
        serverId, access.name(), null, "Create event",
        entries.stream().map(candidate -> new DiscordDirectory.Option(String.valueOf(candidate.getId()), describe(candidate))).toList(),
        entry.getId(), entry.getTemplateKey(), entry.getGuildName(), entry.getHosterUserId(), entry.getHosterUserId(),
        entry.getStart().atZone(ZoneId.systemDefault()).toLocalDate() + "T20:00", entry.getDuration(),
        null, null, null, 0, false, false), error);
  }

  @POST
  @Path("servers/{serverId}/events")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response create(@PathParam("serverId") String serverId, @BeanParam Fields fields) {
    WebAccessService.ServerAccess access = accessService.serverAccess(serverId, currentUser.id());
    String formPath = "/servers/" + serverId + "/events/new" + (fields.entryId == null ? "" : "?entryId=" + fields.entryId);
    try {
      RaidCalendarEntry entry = creatableEntries(serverId, access).stream()
          .filter(candidate -> candidate.getId().equals(fields.entryId))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Choose a free calendar entry you may create an event for."));
      Instant start = WebForms.dateTime(fields.start);
      CreateEventCommand command = details(serverId, fields)
          .calendarEntryId(entry.getId())
          .creatorUserId(currentUser.id())
          .creatorIsGuildManager(access.manageGuilds())
          .guildName(entry.getGuildName())
          .hosterUserId(entry.getHosterUserId())
          .templateKey(entry.getTemplateKey())
          .start(start)
          .end(start.plus(java.time.Duration.between(entry.getStart(), entry.getEnd())))
          .build();
      Event created = eventService.createEvent(command);
      return WebForms.redirect("/events/" + created.getId(), "message", "Event created.");
    } catch (IllegalArgumentException | IllegalStateException ex) {
      return Response.seeOther(URI.create(formPath + (formPath.contains("?") ? "&" : "?") + "error="
          + java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8))).build();
    }
  }

  @GET
  @Path("events/{eventId}/edit")
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance editEvent(@PathParam("eventId") Long eventId, @QueryParam("error") String error) {
    Event event = requireEditableEvent(eventId);
    String serverId = event.getGuild().getServerId();
    return Templates.eventForm(currentUser.displayName(), form(
        serverId, accessService.serverAccess(serverId, currentUser.id()).name(), eventId, "Edit event", List.of(), null,
        event.getTemplate().getKey(), event.getGuild().getName(), event.getHoster().getUserId(),
        event.getEventAdmin().getUserId(), WebForms.dateTimeInput(event.getStart()), event.getDuration(),
        event.getCategoryId(), event.getSignUpChannelId(), event.getVoiceChannelId(),
        event.getDonation(), event.isDonationPerWin(), event.isTeamsRequireApproval()), error);
  }

  @POST
  @Path("events/{eventId}/edit")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response update(@PathParam("eventId") Long eventId, @BeanParam Fields fields) {
    Event event = requireEditableEvent(eventId);
    String serverId = event.getGuild().getServerId();
    return WebForms.run("/events/" + eventId, "/events/" + eventId + "/edit", () -> {
      Instant start = WebForms.dateTime(fields.start);
      eventService.updateEvent(eventId, details(serverId, fields)
          .creatorUserId(currentUser.id())
          .guildName(WebForms.required(fields.guildName, "guild"))
          .hosterUserId(WebForms.required(fields.hosterUserId, "key hoster"))
          .templateKey(WebForms.required(fields.templateKey, "template"))
          .start(start)
          .end(start.plus(WebForms.duration(fields.duration)))
          .build());
      return "Event updated.";
    });
  }

  /** Fields both forms share: channels as the server's settings require them, donation, admin, approval. */
  private CreateEventCommand.CreateEventCommandBuilder details(String serverId, Fields fields) {
    Server server = serverService.findServerById(serverId);
    CreateEventCommand.CreateEventCommandBuilder builder = CreateEventCommand.builder()
        .serverId(serverId)
        .eventAdminUserId(WebForms.required(fields.eventAdminUserId, "event admin"))
        .donation(WebForms.nonNegative(fields.donation, "donation"))
        .donationPerWin("per-win".equals(fields.donationType))
        .teamsRequireApproval("true".equals(fields.teamsRequireApproval));
    if (requiresCategory(server)) {
      builder.categoryId(WebForms.required(fields.categoryId, "category"));
    }
    if (!server.isManageSignupChannels()) {
      builder.signUpChannelId(WebForms.required(fields.signUpChannelId, "sign-up channel"));
    }
    if (!server.isManageVoiceChannels()) {
      builder.voiceChannelId(WebForms.required(fields.voiceChannelId, "voice channel"));
    }
    return builder;
  }

  private EventForm form(
      String serverId, String serverName, Long eventId, String title, List<DiscordDirectory.Option> entries,
      Long entryId, String templateKey, String guildName, String hosterUserId, String eventAdminUserId,
      String start, String duration, String categoryId, String signUpChannelId, String voiceChannelId,
      int donation, boolean donationPerWin, boolean teamsRequireApproval) {
    Server server = serverService.findServerById(serverId);
    boolean askCategory = requiresCategory(server);
    return new EventForm(
        serverId, serverName, eventId, title, entries, entryId,
        templateService.findAllTemplates(serverId).stream()
            .sorted(Comparator.comparing(EventTemplate::getName, String.CASE_INSENSITIVE_ORDER))
            .map(template -> new DiscordDirectory.Option(template.getKey(), template.getName())).toList(),
        templateKey,
        guildPort.findGuildByServerId(serverId).stream().map(Guild::getName).sorted().toList(),
        guildName,
        directory.members(serverId),
        hosterUserId,
        eventAdminUserId,
        start,
        WebForms.DURATIONS,
        duration,
        askCategory,
        !server.isManageSignupChannels(),
        !server.isManageVoiceChannels(),
        askCategory ? directory.categories(serverId) : List.of(),
        server.isManageSignupChannels() ? List.of() : directory.textChannels(serverId),
        server.isManageVoiceChannels() ? List.of() : directory.voiceChannels(serverId),
        categoryId, signUpChannelId, voiceChannelId, donation, donationPerWin, teamsRequireApproval);
  }

  /** Free upcoming entries: all of them for guild managers, otherwise only the ones the user hosts. */
  private List<RaidCalendarEntry> creatableEntries(String serverId, WebAccessService.ServerAccess access) {
    calendarService.requireCalendarConfigured(serverId);
    String userId = currentUser.id();
    return calendarService.findUpcomingAvailableEntries(serverId).stream()
        .filter(entry -> access.manageGuilds() || userId.equals(entry.getHosterUserId()))
        .toList();
  }

  private Event requireEditableEvent(Long eventId) {
    Event event = eventService.findEventByIdOptional(eventId).orElseThrow(() -> new NotFoundException("Event not found."));
    WebAccessService.ServerAccess access = accessService.serverAccess(event.getGuild().getServerId(), currentUser.id());
    access.require(access.manageEvents());
    return event;
  }

  private String describe(RaidCalendarEntry entry) {
    return DAY_FORMAT.format(entry.getStart().atZone(ZoneId.systemDefault())) + " · " + entry.getGuildName()
        + " · " + entry.getTemplateKey() + " · " + entry.getDuration() + " · host "
        + directory.memberName(entry.getServerId(), entry.getHosterUserId());
  }

  private static boolean requiresCategory(Server server) {
    return server.isManageSignupChannels() || server.isManageVoiceChannels();
  }
}
