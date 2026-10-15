package de.qf0xb.raidsignupbot.application.service.event;

import de.qf0xb.raidsignupbot.application.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.port.EventUIApplicationPort;
import de.qf0xb.raidsignupbot.application.port.RaidCalendarApplicationPort;
import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildMemberService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateService;
import de.qf0xb.raidsignupbot.domain.model.calendar.RaidCalendarEntry;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventPostponeTest {
  private final EventApplicationPort eventPort = mock(EventApplicationPort.class);
  private final EventUIApplicationPort ui = mock(EventUIApplicationPort.class);
  private final RaidCalendarApplicationPort calendarPort = mock(RaidCalendarApplicationPort.class);
  private final EventBackupService backups = new EventBackupService();
  private final RaidCalendarService calendar = new RaidCalendarService(calendarPort, eventPort,
      mock(ServerService.class), mock(GuildService.class), mock(EventTemplateApplicationPort.class));
  private final EventService service = new EventService(eventPort, mock(GuildService.class),
      mock(ServerService.class), mock(GuildMemberService.class), mock(TemplateService.class), ui, calendar, backups);

  @Test
  void preservesRaidStateAndMovesLinkedCalendarEntryAcrossDays() {
    Event event = raid(Instant.now().plus(Duration.ofDays(3)));
    event.closeSignup();
    event.setTeamsMessageId("teams");
    event.setMessageId("signup");
    event.setChannelsExist(true);
    event.setOneHourBeforeEmitted(true);
    event.setThirtyMinutesBeforeEmitted(true);
    event.setFifteenMinutesBeforeEmitted(true);
    EventTeam team = mock(EventTeam.class);
    event.addTeam(team);
    Instant oldStart = event.getStart();
    Instant oldEnd = event.getEnd();
    RaidCalendarEntry linked = entry(1L, oldStart, 7L);
    when(calendarPort.findByServerId("server")).thenReturn(List.of(linked));
    backups.registerOffers(event, new TeamGeneratorResult(List.of(), List.of(
        new EventMemberSignup(new GuildMember("backup"), "fire"))));
    var offer = backups.findOffers(7L).getFirst();
    backups.respond(offer.offerId(), "backup", true);

    Event result = service.postponeEvent("server", 7L, 1440);

    assertSame(event, result);
    assertEquals(oldStart.plus(Duration.ofDays(1)), result.getStart());
    assertEquals(oldEnd.plus(Duration.ofDays(1)), result.getEnd());
    assertEquals(List.of(team), result.getTeams());
    assertFalse(result.isSignupOpen());
    assertTrue(result.isTeamsGenerated());
    assertTrue(result.isChannelsExist());
    assertEquals("signup", result.getMessageId());
    assertFalse(result.isOneHourBeforeEmitted());
    assertFalse(result.isThirtyMinutesBeforeEmitted());
    assertFalse(result.isFifteenMinutesBeforeEmitted());
    assertEquals(List.of(offer), backups.findAcceptedOffers(7L));
    verify(ui).updateEvent(event, event);
    verify(ui).updateTeams(event, new TeamGeneratorResult(List.of(team), List.of()));
    verify(ui).announcePostponement(event, oldStart);
    verify(ui, never()).requestBackups(any(), any());
    ArgumentCaptor<RaidCalendarEntry> saved = ArgumentCaptor.forClass(RaidCalendarEntry.class);
    verify(calendarPort).save(saved.capture());
    assertEquals(1L, saved.getValue().getId());
    assertEquals(7L, saved.getValue().getEventId());
    assertEquals(result.getStart().atZone(ZoneId.systemDefault()).toLocalDate(),
        saved.getValue().getStart().atZone(ZoneId.systemDefault()).toLocalDate());
    assertEquals(Duration.ofHours(2), Duration.between(saved.getValue().getStart(), saved.getValue().getEnd()));
  }

  @Test
  void smallDelayKeepsAlreadySentRemindersAndPendingApproval() {
    Event event = raid(Instant.now().plusSeconds(300));
    event.setSignupOpen(false);
    event.setApprovalPending(true);
    event.setOneHourBeforeEmitted(true);
    event.setThirtyMinutesBeforeEmitted(true);
    event.setFifteenMinutesBeforeEmitted(true);

    service.postponeEvent("server", 7L, 5);

    assertTrue(event.isOneHourBeforeEmitted());
    assertTrue(event.isThirtyMinutesBeforeEmitted());
    assertTrue(event.isFifteenMinutesBeforeEmitted());
    assertTrue(event.isApprovalPending());
    assertFalse(event.isSignupOpen());
    assertFalse(event.isTeamsGenerated());
    verify(ui, never()).publishTeams(any(), any());
  }

  @Test
  void rejectsConflictingCalendarDayBeforeChangingTheRaid() {
    Instant start = Instant.now().plus(Duration.ofDays(3));
    Event event = raid(start);
    when(calendarPort.findByServerId("server"))
        .thenReturn(List.of(entry(1L, start, 7L), entry(2L, start.plus(Duration.ofDays(1)), null)));

    assertThrows(IllegalArgumentException.class, () -> service.postponeEvent("server", 7L, 1440));

    assertEquals(start, event.getStart());
    verifyNoInteractions(ui);
    verify(calendarPort, never()).save(any());
    verify(eventPort, never()).save(any());
  }

  @Test
  void rejectsConflictingPersistedEventWithoutCalendarEntry() {
    Instant start = Instant.now().plus(Duration.ofDays(3));
    Event event = raid(start);
    Event other = Event.builder().id(8L).guild(event.getGuild()).start(start.plus(Duration.ofDays(1))).build();
    when(eventPort.findAllEvents()).thenReturn(Set.of(event, other));

    assertThrows(IllegalArgumentException.class, () -> service.postponeEvent("server", 7L, 1440));
    assertEquals(start, event.getStart());
    verifyNoInteractions(ui);
  }

  @Test
  void rejectsInvalidDelayWrongServerAndStartedRaids() {
    raid(Instant.now().plusSeconds(3600));
    assertThrows(IllegalArgumentException.class, () -> service.postponeEvent("server", 7L, 0));
    assertThrows(IllegalArgumentException.class, () -> service.postponeEvent("server", 7L, -5));
    assertThrows(IllegalArgumentException.class, () -> service.postponeEvent("other", 7L, 5));
    raid(Instant.now().minusSeconds(60));
    assertThrows(IllegalArgumentException.class, () -> service.postponeEvent("server", 7L, 5));
    verifyNoInteractions(ui);
    verify(calendarPort, never()).save(any());
  }

  @Test
  void extendsBackupOfferExpiryWithoutReplacingButtonsOrResponses() {
    Event event = raid(Instant.now().minusSeconds(7200));
    backups.registerOffers(event, new TeamGeneratorResult(List.of(), List.of(
        new EventMemberSignup(new GuildMember("backup"), "fire"))));
    var offer = backups.findOffers(7L).getFirst();
    assertThrows(IllegalStateException.class, () -> backups.respond(offer.offerId(), "backup", true));
    backups.extendOffers(7L, Instant.now().plusSeconds(3600));
    assertEquals(List.of(offer), backups.respond(offer.offerId(), "backup", true).acceptedOffers());
  }

  private Event raid(Instant start) {
    EventTemplate template = new EventTemplate("raid", "Raid", "", "coin");
    EventStrategy strategy = new EventStrategy("main", "Main");
    strategy.addRole(new EventRole("fire", "Fire", ":fire:", 1));
    template.addStrategy(strategy);
    Event event = Event.builder().id(7L).guild(new Guild("Guild", "server"))
        .template(template).start(start).end(start.plus(Duration.ofHours(2))).build();
    when(eventPort.findById(7L)).thenReturn(Optional.of(event));
    when(eventPort.findAllEvents()).thenReturn(Set.of(event));
    when(eventPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    return event;
  }

  private RaidCalendarEntry entry(Long id, Instant start, Long eventId) {
    return RaidCalendarEntry.builder().id(id).serverId("server").guildName("Guild")
        .hosterUserId("host").templateKey("raid").start(start).end(start.plus(Duration.ofHours(2)))
        .eventId(eventId).build();
  }
}
