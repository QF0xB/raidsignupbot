package de.qf0xb.raidsignupbot.application.service;

import de.qf0xb.raidsignupbot.application.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.application.service.generator.GreedyTeamGeneratorService;
import de.qf0xb.raidsignupbot.application.service.generator.OrToolsTeamGeneratorService;
import de.qf0xb.raidsignupbot.application.service.generator.TeamGeneratorService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DualBoxTeamGeneratorTest {
  static Stream<TeamGeneratorService> generators() {
    return Stream.of(new OrToolsTeamGeneratorService(), new GreedyTeamGeneratorService());
  }

  @ParameterizedTest
  @MethodSource("generators")
  void fillsTwoDifferentPrimaryRolesOnlyWithOptIn(TeamGeneratorService generator) {
    Event event = event(strategy("main", "fire", "support"));
    EventMemberSignup signup = signup("dual", "fire", "support");
    assertTrue(generator.generateTeams(event, List.of(signup)).teams().isEmpty());

    signup.addRoleKey(EventMemberSignup.DUAL_BOX_ROLE_KEY);
    TeamGeneratorResult result = generator.generateTeams(event, List.of(signup));
    assertEquals(1, result.teams().size());
    assertEquals(Map.of("dual", 2L), assignments(result));
    assertTrue(result.unmatchedSignups().isEmpty());
    assertTrue(result.teams().getFirst().isFilled());
  }

  @ParameterizedTest
  @MethodSource("generators")
  void fillsRepeatedRoleSlotsWithoutDuplicatingNormalPlayers(TeamGeneratorService generator) {
    EventStrategy strategy = new EventStrategy("main", "Main");
    strategy.addRole(new EventRole("fire", "Fire", "🔥", 3));
    TeamGeneratorResult result = generator.generateTeams(event(strategy), List.of(
        signup("dual", "fire", EventMemberSignup.DUAL_BOX_ROLE_KEY), signup("single", "fire")));

    assertEquals(1, result.teams().size());
    assertEquals(Map.of("dual", 2L, "single", 1L), assignments(result));
    assertTrue(result.unmatchedSignups().isEmpty());
  }

  @ParameterizedTest
  @MethodSource("generators")
  void sharesTheTwoSlotLimitAcrossStrategies(TeamGeneratorService generator) {
    Event event = event(strategy("first", "fire", "support"), strategy("second", "storm", "heal"));
    TeamGeneratorResult result = generator.generateTeams(event, List.of(
        signup("dual", "fire", "storm", EventMemberSignup.DUAL_BOX_ROLE_KEY),
        signup("support", "support"), signup("healer", "heal")));

    assertEquals(2, result.teams().size());
    assertEquals(Map.of("dual", 2L, "support", 1L, "healer", 1L), assignments(result));
    assertTrue(result.unmatchedSignups().isEmpty());
  }

  @ParameterizedTest
  @MethodSource("generators")
  void neverSelectsAPlayerMoreThanTwice(TeamGeneratorService generator) {
    TeamGeneratorResult result = generator.generateTeams(event(strategy("main", "fire")), List.of(
        signup("dual", "fire", EventMemberSignup.DUAL_BOX_ROLE_KEY)));

    assertEquals(2, result.teams().size());
    assertEquals(Map.of("dual", 2L), assignments(result));
  }

  @ParameterizedTest
  @MethodSource("generators")
  void prefersAnotherPlayerOverASecondSlotWhenBothFit(TeamGeneratorService generator) {
    Event event = event(strategy("main", "fire", "support"));
    EventMemberSignup dual = signup("dual", "fire", "support", EventMemberSignup.DUAL_BOX_ROLE_KEY);
    dual.getGuildMember().setPriority(1000);
    TeamGeneratorResult result = generator.generateTeams(event, List.of(dual, signup("single", "support")));

    assertEquals(1, result.teams().size());
    assertEquals(Map.of("dual", 1L, "single", 1L), assignments(result));
    assertTrue(result.unmatchedSignups().isEmpty());
  }

  @ParameterizedTest
  @MethodSource("generators")
  void utilityCanBeCoveredByTheSecondPrimaryRole(TeamGeneratorService generator) {
    EventStrategy strategy = strategy("main", "fire", "support");
    EventUtilityRole utility = new EventUtilityRole("hex", "Hex", "☀️", 1);
    utility.addEligiblePrimaryRole("support");
    strategy.addRole(utility);
    TeamGeneratorResult result = generator.generateTeams(event(strategy), List.of(
        signup("dual", "fire", "support", "hex", EventMemberSignup.DUAL_BOX_ROLE_KEY)));

    assertEquals(1, result.teams().size());
    assertTrue(result.teams().getFirst().isFilled());
    assertEquals("dual", result.teams().getFirst().getUtilityAssignments().getFirst().getMember().getUserId());
  }

  @ParameterizedTest
  @MethodSource("generators")
  void dualBoxReactionAloneDoesNotCreateARoleSignup(TeamGeneratorService generator) {
    EventMemberSignup packageOnly = signup("package-only", EventMemberSignup.DUAL_BOX_ROLE_KEY);
    TeamGeneratorResult result = generator.generateTeams(event(strategy("main", "fire")), List.of(packageOnly));
    assertTrue(result.teams().isEmpty());
    assertEquals(List.of(packageOnly), result.unmatchedSignups());
  }

  private Map<String, Long> assignments(TeamGeneratorResult result) {
    return result.teams().stream().flatMap(team -> team.getSlots().stream())
        .collect(Collectors.groupingBy(slot -> slot.getMember().getUserId(), Collectors.counting()));
  }

  @ParameterizedTest
  @MethodSource("generators")
  void keepsBothBoxesOnTheSameTeamWhenAllPlayersCanStillBeSeated(TeamGeneratorService generator) {
    Event event = event(strategy("main", "fire", "support"));
    TeamGeneratorResult result = generator.generateTeams(event, List.of(
        signup("dual", "fire", "support", EventMemberSignup.DUAL_BOX_ROLE_KEY),
        signup("fire", "fire"), signup("support", "support")));

    assertEquals(2, result.teams().size());
    assertEquals(Map.of("dual", 2L, "fire", 1L, "support", 1L), assignments(result));
    assertTrue(result.teams().stream().anyMatch(team -> team.getSlots().stream()
        .allMatch(slot -> slot.getMember().getUserId().equals("dual"))));
    assertTrue(result.unmatchedSignups().isEmpty());
  }

  @ParameterizedTest
  @MethodSource("generators")
  void groupingRespectsTheUtilitySignupsOfBothPlayers(TeamGeneratorService generator) {
    EventStrategy strategy = strategy("main", "fire", "support");
    EventUtilityRole utility = new EventUtilityRole("hex", "Hex", "☀️", 1);
    utility.addEligiblePrimaryRole("support");
    strategy.addRole(utility);
    TeamGeneratorResult result = generator.generateTeams(event(strategy), List.of(
        signup("dual", "fire", "support", "hex", EventMemberSignup.DUAL_BOX_ROLE_KEY),
        signup("fire", "fire"), signup("support", "support", "hex")));

    assertEquals(2, result.teams().size());
    assertTrue(result.teams().stream().allMatch(team -> team.isFilled()));
    assertTrue(result.teams().stream().anyMatch(team -> team.getSlots().stream()
        .allMatch(slot -> slot.getMember().getUserId().equals("dual"))));
    assertTrue(result.teams().stream().flatMap(team -> team.getUtilityAssignments().stream())
        .noneMatch(assignment -> assignment.getMember().getUserId().equals("fire")));
  }

  private EventMemberSignup signup(String id, String... roles) {
    return new EventMemberSignup(new GuildMember(id), roles);
  }

  private EventStrategy strategy(String key, String... roles) {
    EventStrategy strategy = new EventStrategy(key, key);
    for (String role : roles) {
      strategy.addRole(new EventRole(role, role, "🔥", 1));
    }
    return strategy;
  }

  private Event event(EventStrategy... strategies) {
    EventTemplate template = new EventTemplate("raid", "Raid", "", "coin");
    for (EventStrategy strategy : strategies) {
      template.addStrategy(strategy);
    }
    return Event.builder().template(template).build();
  }
}
