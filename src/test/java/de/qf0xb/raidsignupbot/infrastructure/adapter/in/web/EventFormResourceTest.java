package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import de.qf0xb.raidsignupbot.application.dto.CreateEventCommand;
import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateService;
import de.qf0xb.raidsignupbot.domain.model.calendar.RaidCalendarEntry;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventFormResourceTest {
  private final CurrentUser currentUser = mock(CurrentUser.class);
  private final WebAccessService accessService = mock(WebAccessService.class);
  private final EventService eventService = mock(EventService.class);
  private final RaidCalendarService calendarService = mock(RaidCalendarService.class);
  private final ServerService serverService = mock(ServerService.class);
  private final EventFormResource resource = new EventFormResource(currentUser, accessService, eventService,
      calendarService, serverService, mock(TemplateService.class), mock(GuildApplicationPort.class),
      mock(DiscordDirectory.class));
  private final LocalDate day = LocalDate.now(ZoneId.systemDefault()).plusDays(3);
  private final RaidCalendarEntry entry = RaidCalendarEntry.builder()
      .id(5L).serverId("server").guildName("Guild").hosterUserId("host").templateKey("raid")
      .start(day.atStartOfDay(ZoneId.systemDefault()).toInstant())
      .end(day.atStartOfDay(ZoneId.systemDefault()).toInstant().plus(Duration.ofMinutes(90)))
      .build();

  @BeforeEach
  void setUp() {
    when(calendarService.findUpcomingAvailableEntries("server")).thenReturn(List.of(entry));
    Server server = new Server("server");
    server.setManageSignupChannels(true);
    server.setManageVoiceChannels(true);
    when(serverService.findServerById("server")).thenReturn(server);
    when(eventService.createEvent(any())).thenReturn(Event.builder().id(9L).build());
  }

  @Test
  void theEntryHostCreatesTheEventWithTheEntrysFixedDetails() {
    signedIn("host", false);

    Response response = resource.create("server", fields(5L));

    ArgumentCaptor<CreateEventCommand> command = ArgumentCaptor.forClass(CreateEventCommand.class);
    verify(eventService).createEvent(command.capture());
    assertEquals("/events/9", response.getLocation().getPath());
    assertEquals(5L, command.getValue().getCalendarEntryId());
    assertEquals("Guild", command.getValue().getGuildName());
    assertEquals("host", command.getValue().getHosterUserId());
    assertEquals("raid", command.getValue().getTemplateKey());
    assertEquals(day.atTime(20, 15).atZone(ZoneId.systemDefault()).toInstant(), command.getValue().getStart());
    assertEquals(Duration.ofMinutes(90), Duration.between(command.getValue().getStart(), command.getValue().getEnd()));
    assertEquals("category", command.getValue().getCategoryId());
    assertFalse(command.getValue().isCreatorIsGuildManager());
  }

  @Test
  void otherUsersCannotCreateEventsForSomeoneElsesEntry() {
    signedIn("someone", false);

    Response response = resource.create("server", fields(5L));

    verify(eventService, never()).createEvent(any());
    assertTrue(response.getLocation().toString().contains("error="));
  }

  @Test
  void guildManagersMayCreateEventsForAnyEntry() {
    signedIn("manager", true);

    resource.create("server", fields(5L));

    ArgumentCaptor<CreateEventCommand> command = ArgumentCaptor.forClass(CreateEventCommand.class);
    verify(eventService).createEvent(command.capture());
    assertTrue(command.getValue().isCreatorIsGuildManager());
  }

  private void signedIn(String userId, boolean guildManager) {
    when(currentUser.id()).thenReturn(userId);
    when(accessService.serverAccess("server", userId))
        .thenReturn(new WebAccessService.ServerAccess("server", "Server", false, guildManager, false, false));
  }

  private EventFormResource.Fields fields(Long entryId) {
    EventFormResource.Fields fields = new EventFormResource.Fields();
    fields.entryId = entryId;
    fields.start = day + "T20:15";
    fields.categoryId = "category";
    fields.donation = "10";
    fields.donationType = "one-time";
    fields.eventAdminUserId = "host";
    fields.teamsRequireApproval = "true";
    return fields;
  }
}
