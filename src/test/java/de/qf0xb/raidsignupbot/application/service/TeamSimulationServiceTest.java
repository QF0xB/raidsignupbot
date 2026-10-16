package de.qf0xb.raidsignupbot.application.service;

import de.qf0xb.raidsignupbot.application.service.generator.TeamSimulationService;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.event.EventSlot;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeamSimulationServiceTest {
  private final TeamSimulationService service = new TeamSimulationService();

  @Test
  void buildsTeamsFromManySimulatedPlayers() {
    var simulation = service.simulate(template(), settings(60, 42));

    assertEquals(60, simulation.signups().size());
    assertFalse(simulation.result().teams().isEmpty());
    simulation.result().teams().forEach(team -> team.getSlots().stream().filter(EventSlot::isAssigned)
        .forEach(slot -> assertTrue(signupOf(simulation.signups(), slot.getMember().getUserId()).wantsRole(slot.getKey()),
            "players are only placed in roles they reacted with")));
  }

  @Test
  void theSameSeedProducesTheSameSignups() {
    List<String> first = roles(service.simulate(template(), settings(30, 7)).signups());
    List<String> second = roles(service.simulate(template(), settings(30, 7)).signups());
    List<String> other = roles(service.simulate(template(), settings(30, 8)).signups());

    assertEquals(first, second);
    assertNotEquals(first, other);
  }

  @Test
  void rejectsInvalidSettingsAndEmptyTemplates() {
    assertThrows(IllegalArgumentException.class, () -> settings(0, 1));
    assertThrows(IllegalArgumentException.class,
        () -> new TeamSimulationService.Settings(10, 3, 2, 0, 0, 0, false, 1));
    assertThrows(IllegalArgumentException.class,
        () -> service.simulate(new EventTemplate("empty", "Empty", "", "coin"), settings(10, 1)));
  }

  private static TeamSimulationService.Settings settings(int players, long seed) {
    return new TeamSimulationService.Settings(players, 1, 3, 30, 10, 5, true, seed);
  }

  private static EventTemplate template() {
    EventTemplate template = new EventTemplate("raid", "Raid", "", "coin");
    EventStrategy strategy = new EventStrategy("main", "Main");
    strategy.addRole(new EventRole("fire", "Fire", "🔥", 1));
    strategy.addRole(new EventRole("ice", "Ice", "❄️", 1));
    strategy.addRole(new EventRole("storm", "Storm", "⚡", 2));
    EventUtilityRole hex = new EventUtilityRole("hex", "Hex", "🌀", 1);
    hex.addEligiblePrimaryRole("fire");
    hex.addEligiblePrimaryRole("ice");
    strategy.addRole(hex);
    template.addStrategy(strategy);
    return template;
  }

  private static EventMemberSignup signupOf(List<EventMemberSignup> signups, String userId) {
    return signups.stream().filter(signup -> signup.getGuildMember().getUserId().equals(userId)).findFirst().orElseThrow();
  }

  private static List<String> roles(List<EventMemberSignup> signups) {
    return signups.stream().map(signup -> signup.getRoleKeys().stream().sorted().toList().toString()).toList();
  }
}
