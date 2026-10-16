package de.qf0xb.raidsignupbot.application.service.event;

import de.qf0xb.raidsignupbot.application.service.guild.RaiderRoleService;
import de.qf0xb.raidsignupbot.application.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.port.EventUIApplicationPort;
import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildMemberService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Logic shared by the slash commands and the web UI. */
class EventServiceWebActionsTest {
  private final EventApplicationPort port = mock(EventApplicationPort.class);
  private final EventUIApplicationPort ui = mock(EventUIApplicationPort.class);
  private final RaidCalendarService calendar = mock(RaidCalendarService.class);
  private final EventService service = new EventService(port, mock(GuildService.class), mock(ServerService.class),
      mock(GuildMemberService.class), mock(TemplateService.class), ui, calendar, new EventBackupService(), mock(RaiderRoleService.class));

  @Test
  void rejectingReopensSignupAndAnnouncesWhoRejected() throws Exception {
    Event event = stored(Event.builder().id(7L).signupOpen(false).approvalPending(true).build());
    pendingProposals().put(7L, new TeamGeneratorResult(List.of(), List.of()));

    service.rejectGeneratedTeams(7L, "admin");

    assertTrue(event.isSignupOpen());
    assertFalse(event.isApprovalPending());
    verify(ui).announceTeamsRejected(event, "admin");
  }

  @Test
  void closingAnEventSuppressesRemainingRemindersAndRemovesItsChannels() {
    Event event = stored(Event.builder().id(7L).build());

    service.closeEvent(7L);

    assertFalse(event.isSignupOpen());
    assertTrue(event.isOneHourBeforeEmitted() && event.isThirtyMinutesBeforeEmitted()
        && event.isFifteenMinutesBeforeEmitted() && event.isOneHourAfterEmitted());
    verify(ui).deleteEvent(event);
    verify(port).save(event);
  }

  @Test
  void deletingEverywhereFreesTheCalendarAndCleansUpDiscord() {
    Event event = stored(Event.builder().id(7L).build());

    service.deleteEventEverywhere(7L);

    verify(calendar).releaseEvent(7L);
    verify(port).deleteById(7L);
    verify(ui).removeEventFromDiscord(event);
  }

  private Event stored(Event event) {
    when(port.findById(event.getId())).thenReturn(Optional.of(event));
    when(port.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    return event;
  }

  @SuppressWarnings("unchecked")
  private Map<Long, TeamGeneratorResult> pendingProposals() throws Exception {
    var field = EventService.class.getDeclaredField("pendingApprovalResults");
    field.setAccessible(true);
    return (Map<Long, TeamGeneratorResult>) field.get(service);
  }
}
