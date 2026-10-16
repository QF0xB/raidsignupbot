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
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventSlotSwapTest {
  private final EventApplicationPort port = mock(EventApplicationPort.class);
  private final EventUIApplicationPort ui = mock(EventUIApplicationPort.class);
  private final GuildMemberService members = mock(GuildMemberService.class);
  private final EventService service = new EventService(port, mock(GuildService.class), mock(ServerService.class),
      members, mock(TemplateService.class), ui, mock(RaidCalendarService.class), new EventBackupService(),
      mock(RaiderRoleService.class));
  private final GuildMember dual = new GuildMember("dual");
  private final GuildMember single = new GuildMember("single");
  private final GuildMember replacement = new GuildMember("replacement");

  @Test
  void requiresASlotWhenThePlayerHasTwoBoxesAndChangesNothingOnFailure() {
    Event event = event();
    String error = assertThrows(IllegalArgumentException.class,
        () -> service.swapTeamMembers(7L, "dual", "single")).getMessage();
    assertTrue(error.contains("1:1") && error.contains("1:2"), error);
    assertEquals(Map.of("dual", 2L, "single", 1L), counts(event));
    verify(port, never()).save(any());
    verifyNoInteractions(ui);
  }

  @Test
  void swapsOnlyTheSelectedBoxAndItsUtilityResponsibilities() {
    Event event = event();
    EventTeam first = event.getTeams().getFirst();
    EventTeam second = event.getTeams().get(1);
    service.swapTeamMembers(7L, "dual", "single", "1:2", null);

    assertEquals("dual", first.getSlots().getFirst().getMember().getUserId());
    assertEquals("single", first.getSlots().get(1).getMember().getUserId());
    assertEquals("dual", second.getSlots().getFirst().getMember().getUserId());
    assertEquals("dual", first.getUtilityAssignments().getFirst().getMember().getUserId());
    assertEquals("single", first.getUtilityAssignments().get(1).getMember().getUserId());
    assertEquals(Map.of("dual", 2L, "single", 1L), counts(event));
    verify(ui).updateTeams(event, new TeamGeneratorResult(event.getTeams(), List.of()));
  }

  @Test
  void substitutesOnlyOneBoxForAnUnassignedPlayer() {
    Event event = event();
    service.swapTeamMembers(7L, "dual", "replacement", "1:1", null);
    assertEquals(Map.of("dual", 1L, "single", 1L, "replacement", 1L), counts(event));
    assertEquals("replacement", event.getTeams().getFirst().getUtilityAssignments().getFirst().getMember().getUserId());
    assertEquals("dual", event.getTeams().getFirst().getUtilityAssignments().get(1).getMember().getUserId());
  }

  @Test
  void rejectsASlotOwnedByAnotherPlayerBeforeChangingAnything() {
    Event event = event();
    assertThrows(IllegalArgumentException.class,
        () -> service.swapTeamMembers(7L, "dual", "single", "2:1", null));
    assertEquals(Map.of("dual", 2L, "single", 1L), counts(event));
    verify(port, never()).save(any());
  }

  @Test
  void ordinarySingleSlotSwapDoesNotNeedSlotArguments() {
    Event event = event();
    service.swapTeamMembers(7L, "single", "replacement");
    assertEquals(Map.of("dual", 2L, "replacement", 1L), counts(event));
  }

  @Test
  @SuppressWarnings("unchecked")
  void approvalSwapsAreAlsoSlotSpecificAndPreserveThePendingProposal() throws Exception {
    Event event = event();
    event.setTeamsGenerated(false);
    event.setApprovalPending(true);
    var field = EventService.class.getDeclaredField("pendingApprovalResults");
    field.setAccessible(true);
    Map<Long, TeamGeneratorResult> proposals = (Map<Long, TeamGeneratorResult>) field.get(service);
    proposals.put(7L, new TeamGeneratorResult(event.getTeams(), List.of()));

    service.swapTeamsForApproval(7L, "dual", "single", "1:2", null);

    assertEquals(Map.of("dual", 2L, "single", 1L), counts(event));
    assertEquals("single", event.getTeams().getFirst().getSlots().get(1).getMember().getUserId());
    assertTrue(event.isApprovalPending());
    assertFalse(event.isTeamsGenerated());
    verify(ui).sendTeamsForApproval(event, proposals.get(7L));
    verify(ui, never()).publishTeams(any(), any());
  }

  @Test
  void swappingTwoDualBoxPlayersCanSelectOneBoxOfEach() {
    Event event = event();
    EventTeam second = event.getTeams().get(1);
    EventTeam another = new EventTeam(second.getStrategy());
    another.assign("other", single);
    event.addTeam(another);
    service.swapTeamMembers(7L, "dual", "single", "1:1", "2:1");
    assertEquals(Map.of("dual", 2L, "single", 2L), counts(event));
    assertEquals("dual", event.getTeams().getFirst().getSlots().get(1).getMember().getUserId());
    assertEquals("single", another.getSlots().getFirst().getMember().getUserId());
  }

  private Event event() {
    EventStrategy first = new EventStrategy("main", "Main");
    first.addRole(new EventRole("fire", "Fire", "🔥", 1));
    first.addRole(new EventRole("support", "Support", "☀️", 1));
    EventUtilityRole fireUtility = new EventUtilityRole("fire-util", "Fire utility", "🔥", 1);
    fireUtility.addEligiblePrimaryRole("fire");
    first.addRole(fireUtility);
    EventUtilityRole supportUtility = new EventUtilityRole("support-util", "Support utility", "☀️", 1);
    supportUtility.addEligiblePrimaryRole("support");
    first.addRole(supportUtility);
    EventTeam firstTeam = new EventTeam(first);
    firstTeam.assign("fire", dual);
    firstTeam.assign("support", dual);
    firstTeam.assignUtility("fire-util", dual);
    firstTeam.assignUtility("support-util", dual);
    EventStrategy second = new EventStrategy("other", "Other");
    second.addRole(new EventRole("other", "Other", "🔥", 1));
    EventTeam secondTeam = new EventTeam(second);
    secondTeam.assign("other", single);
    Event event = Event.builder().id(7L).teamsGenerated(true).build();
    event.addTeam(firstTeam);
    event.addTeam(secondTeam);
    when(port.findById(7L)).thenReturn(Optional.of(event));
    when(port.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(members.findGuildMemberById("dual")).thenReturn(dual);
    when(members.findGuildMemberById("single")).thenReturn(single);
    when(members.findGuildMemberById("replacement")).thenReturn(replacement);
    return event;
  }

  private Map<String, Long> counts(Event event) {
    return event.getTeams().stream().flatMap(team -> team.getSlots().stream())
        .collect(Collectors.groupingBy(slot -> slot.getMember().getUserId(), Collectors.counting()));
  }
}
