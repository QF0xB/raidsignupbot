package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.calendar.RaidCalendarPublisher;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import net.dv8tion.jda.api.JDA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventResourceTest {
  private final EventService eventService = mock(EventService.class);
  private final WebAccessService accessService = mock(WebAccessService.class);
  private final CurrentUser currentUser = mock(CurrentUser.class);
  private final RaidCalendarPublisher calendarPublisher = mock(RaidCalendarPublisher.class);
  private final ServerService serverService = mock(ServerService.class);
  private final EventResource resource = new EventResource(
      currentUser, accessService, eventService, new EventViewFactory(mock(JDA.class)), calendarPublisher, serverService);
  private Event event;

  @BeforeEach
  void setUp() {
    EventStrategy strategy = new EventStrategy("main", "Main");
    strategy.addRole(new EventRole("fire", "Fire", "🔥", 2));
    EventTeam team = new EventTeam(strategy);
    team.assign("fire", new GuildMember("alice"));
    team.assign("fire", new GuildMember("bob"));
    event = Event.builder().id(7L).guild(new Guild("Guild", "server")).teamsGenerated(true).build();
    event.addTeam(team);
    when(eventService.findEventByIdOptional(7L)).thenReturn(Optional.of(event));
    when(currentUser.id()).thenReturn("manager");
    allow(true, false, false);
  }

  @Test
  void swapsTheSelectedSlotsOfThePublishedTeams() {
    Response response = resource.swap(7L, "1:1", "1:2");

    verify(eventService).swapTeamMembers(7L, "alice", "bob", "1:1", "1:2");
    assertEquals(303, response.getStatus());
    assertTrue(response.getLocation().toString().contains("message="));
  }

  @Test
  void onlyTheEventAdminEditsAPendingProposal() {
    event.setApprovalPending(true);
    assertThrows(ForbiddenException.class, () -> resource.swap(7L, "1:2", "1:1"));

    allow(false, false, true);
    resource.swap(7L, "1:2", "1:1");

    verify(eventService).swapTeamsForApproval(7L, "bob", "alice", "1:2", "1:1");
    verify(eventService, never()).swapTeamMembers(any(), any(), any(), any(), any());
  }

  @Test
  void approvalDecisionsRequireTheEventAdmin() {
    assertThrows(ForbiddenException.class, () -> resource.approve(7L));
    assertThrows(ForbiddenException.class, () -> resource.reject(7L));

    allow(false, false, true);
    resource.approve(7L);
    resource.reject(7L);

    verify(eventService).approveGeneratedTeams(7L);
    verify(eventService).rejectGeneratedTeams(7L, "manager");
  }

  @Test
  void reportsUnassignedSlotsWithoutCallingTheService() {
    Response response = resource.swap(7L, "1:1", "3:1");

    verify(eventService, never()).swapTeamMembers(any(), any(), any(), any(), any());
    assertTrue(response.getLocation().toString().contains("error="));
  }

  @Test
  void postponesByWholeDaysAndRefreshesTheConfiguredCalendar() {
    Server server = new Server("server");
    server.setRaidCalendarChannelId("calendar");
    when(serverService.findServerById("server")).thenReturn(server);

    resource.postpone(7L, 2);

    verify(eventService).postponeEvent("server", 7L, 2 * 24 * 60);
    verify(calendarPublisher).publish("server");
  }

  @Test
  void rejectsInvalidPostponementsBeforeCallingTheService() {
    Response response = resource.postpone(7L, 0);

    verify(eventService, never()).postponeEvent(any(), any(), anyInt());
    assertTrue(response.getLocation().toString().contains("error="));
  }

  @Test
  void managerActionsUseTheSharedServiceMethods() {
    resource.closeSignup(7L);
    resource.removeSignup(7L, "bob");
    resource.close(7L);

    verify(eventService).generateTeamsAndCloseSignup(7L, null);
    verify(eventService).removeSignupAndRegenerateTeams(7L, "bob");
    verify(eventService).closeEvent(7L);
  }

  @Test
  void deletingRequiresAdminAccess() {
    assertThrows(ForbiddenException.class, () -> resource.delete(7L));

    allow(true, true, false);
    Response response = resource.delete(7L);

    verify(eventService).deleteEventEverywhere(7L);
    assertEquals("/", response.getLocation().getPath());
  }

  @Test
  void rejectsUsersWithoutAccessToTheEvent() {
    allow(false, false, false);

    assertThrows(ForbiddenException.class, () -> resource.show(7L, null, null));
    assertThrows(ForbiddenException.class, () -> resource.close(7L));
    verifyNoMoreInteractions(ignoreStubs(eventService));
  }

  private void allow(boolean manage, boolean admin, boolean eventAdmin) {
    when(accessService.eventPermissions(event, "manager"))
        .thenReturn(new WebAccessService.EventPermissions(manage, admin, eventAdmin));
  }
}
