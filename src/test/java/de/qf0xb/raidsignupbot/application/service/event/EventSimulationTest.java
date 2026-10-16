package de.qf0xb.raidsignupbot.application.service.event;

import de.qf0xb.raidsignupbot.application.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.port.EventUIApplicationPort;
import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.application.service.generator.TeamSimulationService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildMemberService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildService;
import de.qf0xb.raidsignupbot.application.service.guild.RaiderRoleService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.event.EventSlot;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.guild.SimulatedPlayer;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventSimulationTest {
  private final EventApplicationPort port = mock(EventApplicationPort.class);
  private final EventUIApplicationPort ui = mock(EventUIApplicationPort.class);
  private final GuildMemberService members = mock(GuildMemberService.class);
  private final EventService service = new EventService(port, mock(GuildService.class), mock(ServerService.class),
      members, mock(TemplateService.class), ui, mock(RaidCalendarService.class), new EventBackupService(),
      mock(RaiderRoleService.class));

  @Test
  void rebuildsPublishedTeamsFromSimulatedAndRealSignups() {
    EventTemplate template = template();
    Event event = Event.builder().id(7L).guild(new Guild("Onyx", "server")).template(template)
        .messageId("signup-message").signupOpen(false).teamsGenerated(true).build();
    when(port.findById(7L)).thenReturn(Optional.of(event));
    when(port.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(members.ensureGuildMember(anyString(), eq("server"), eq("Onyx")))
        .thenAnswer(invocation -> new GuildMember(invocation.getArgument(0)));
    when(ui.closeSignup(event)).thenReturn(List.of(new EventMemberSignup(new GuildMember("real"), "fire", "ice")));
    List<EventMemberSignup> simulated = new TeamSimulationService().createSignups(template,
        new TeamSimulationService.Settings(20, 1, 2, 0, 0, 0, false, 3));

    Event result = service.simulateTeamCreation(7L, simulated, true);

    verify(members, times(20)).ensureGuildMember(anyString(), eq("server"), eq("Onyx"));
    assertTrue(result.isTeamsGenerated());
    assertFalse(result.isSignupOpen());
    ArgumentCaptor<TeamGeneratorResult> published = ArgumentCaptor.forClass(TeamGeneratorResult.class);
    verify(ui).publishTeams(eq(result), published.capture());
    List<String> placed = published.getValue().teams().stream().flatMap(team -> team.getSlots().stream())
        .filter(EventSlot::isAssigned).map(slot -> slot.getMember().getUserId()).toList();
    assertFalse(placed.isEmpty());
    assertTrue(placed.stream().allMatch(id -> id.equals("real") || SimulatedPlayer.isSimulated(id)));
  }

  @Test
  void simulatedIdsNeverMatchRealDiscordIds() {
    assertTrue(SimulatedPlayer.isSimulated(SimulatedPlayer.userId(1)));
    assertEquals("Simulated 007", SimulatedPlayer.displayName(SimulatedPlayer.userId(7)));
    assertFalse(SimulatedPlayer.isSimulated("1501862567549603961"));
    assertFalse(SimulatedPlayer.isSimulated("480285761783201823"));
    assertFalse(SimulatedPlayer.isSimulated("not-a-number"));
    assertFalse(SimulatedPlayer.isSimulated(null));
  }

  private static EventTemplate template() {
    EventTemplate template = new EventTemplate("raid", "Raid", "", "coin");
    EventStrategy strategy = new EventStrategy("main", "Main");
    strategy.addRole(new EventRole("fire", "Fire", "🔥", 2));
    strategy.addRole(new EventRole("ice", "Ice", "❄️", 2));
    template.addStrategy(strategy);
    return template;
  }
}
