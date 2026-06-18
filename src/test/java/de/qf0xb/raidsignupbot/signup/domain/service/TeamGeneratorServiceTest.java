package de.qf0xb.raidsignupbot.signup.domain.service;

import de.qf0xb.raidsignupbot.shared.domain.model.EventRole;
import de.qf0xb.raidsignupbot.shared.domain.model.EventUtilityRole;
import de.qf0xb.raidsignupbot.shared.domain.model.PlayerProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TeamGeneratorServiceTest {
  private final TeamGeneratorService service = new TeamGeneratorService();

  @Test
  void prefersFlexiblePlayersWhenResultsCreateTheSameNumberOfTeams() {
    EventRole slotA = role(1L, "North");
    EventRole slotB = role(2L, "East");
    StrategyTemplate strategy = new StrategyTemplate(1L, "Default");
    strategy.addRole(slotA);
    strategy.addRole(slotB);
    EventTemplate template = new EventTemplate(1L, "Raid");
    template.addStrategy(strategy);
    PlayerSignup flexiblePlayer = new PlayerSignup("flexible");
    flexiblePlayer.addSelectedRole(slotA);
    flexiblePlayer.addSelectedRole(slotB);
    PlayerSignup onlyA = new PlayerSignup("only-a");
    onlyA.addSelectedRole(slotA);
    PlayerSignup onlyB = new PlayerSignup("only-b");
    onlyB.addSelectedRole(slotB);
    EventSignup signup = new EventSignup(1L, "guild", "channel", "message");
    signup.addTemplate(template);
    signup.addOrUpdatePlayerSignup(flexiblePlayer);
    signup.addOrUpdatePlayerSignup(onlyA);
    signup.addOrUpdatePlayerSignup(onlyB);

    TeamGeneratorResult result = service.generateTeams(signup);

    assertEquals(1, result.getTeams().size());
    assertEquals(2, result.getTeams().getFirst().getSlotAssignments().size());
    assertEquals(1, result.getUnmatchedPlayers().size());
    assertFalse(result.getUnmatchedPlayers().contains(flexiblePlayer));
  }

  @Test
  void prefersPriorityPlayersOverFlexibilityWhenResultsCreateTheSameNumberOfTeams() {
    EventRole slotA = role(1L, "North");
    EventRole slotB = role(2L, "East");
    EventRole slotC = role(3L, "West");
    StrategyTemplate strategy = new StrategyTemplate(1L, "Default");
    strategy.addRole(slotA);
    strategy.addRole(slotB);
    EventTemplate template = new EventTemplate(1L, "Raid");
    template.addStrategy(strategy);
    PlayerSignup flexiblePlayer = new PlayerSignup("flexible");
    flexiblePlayer.addSelectedRole(slotA);
    flexiblePlayer.addSelectedRole(slotC);
    PlayerProfile priorityProfile = new PlayerProfile("priority-only-a");
    priorityProfile.setPriorityScore(1);
    PlayerSignup priorityOnlyA = new PlayerSignup(priorityProfile);
    priorityOnlyA.addSelectedRole(slotA);
    PlayerSignup onlyB = new PlayerSignup("only-b");
    onlyB.addSelectedRole(slotB);
    EventSignup signup = new EventSignup(1L, "guild", "channel", "message");
    signup.addTemplate(template);
    signup.addOrUpdatePlayerSignup(flexiblePlayer);
    signup.addOrUpdatePlayerSignup(priorityOnlyA);
    signup.addOrUpdatePlayerSignup(onlyB);

    TeamGeneratorResult result = service.generateTeams(signup);

    assertEquals(1, result.getTeams().size());
    assertEquals(List.of(flexiblePlayer), result.getUnmatchedPlayers());
  }

  @Test
  void updatesProfilePriorityAfterRosterIsAccepted() {
    EventRole slotA = role(1L, "North");
    EventRole slotB = role(2L, "East");
    StrategyTemplate strategy = new StrategyTemplate(1L, "Default");
    strategy.addRole(slotA);
    strategy.addRole(slotB);
    EventTemplate template = new EventTemplate(1L, "Raid");
    template.addStrategy(strategy);
    PlayerProfile matchedAProfile = new PlayerProfile("matched-a");
    matchedAProfile.setPriorityScore(2);
    PlayerProfile matchedBProfile = new PlayerProfile("matched-b");
    matchedBProfile.setPriorityScore(1);
    PlayerProfile unmatchedProfile = new PlayerProfile("unmatched");
    PlayerSignup matchedA = new PlayerSignup(matchedAProfile, List.of(slotA), List.of());
    PlayerSignup matchedB = new PlayerSignup(matchedBProfile, List.of(slotB), List.of());
    PlayerSignup unmatched = new PlayerSignup(unmatchedProfile);
    EventSignup signup = new EventSignup(1L, "guild", "channel", "message");
    signup.addTemplate(template);
    signup.addOrUpdatePlayerSignup(matchedA);
    signup.addOrUpdatePlayerSignup(matchedB);
    signup.addOrUpdatePlayerSignup(unmatched);

    TeamGeneratorResult result = service.generateTeams(signup);
    result.applyPriorityUpdates();

    assertEquals(0, matchedAProfile.getPriorityScore());
    assertEquals(0, matchedBProfile.getPriorityScore());
    assertEquals(1, unmatchedProfile.getPriorityScore());
  }

  @Test
  void createsMultipleCompleteTeams() {
    EventRole slotA = role(1L, "North");
    EventRole slotB = role(2L, "East");
    StrategyTemplate strategy = new StrategyTemplate(1L, "Default");
    strategy.addRole(slotA);
    strategy.addRole(slotB);
    EventTemplate template = new EventTemplate(1L, "Raid");
    template.addStrategy(strategy);
    EventSignup signup = new EventSignup(1L, "guild", "channel", "message");
    signup.addTemplate(template);
    signup.addOrUpdatePlayerSignup(new PlayerSignup("a-1", List.of(slotA), List.of()));
    signup.addOrUpdatePlayerSignup(new PlayerSignup("b-1", List.of(slotB), List.of()));
    signup.addOrUpdatePlayerSignup(new PlayerSignup("a-2", List.of(slotA), List.of()));
    signup.addOrUpdatePlayerSignup(new PlayerSignup("b-2", List.of(slotB), List.of()));

    TeamGeneratorResult result = service.generateTeams(signup);

    assertEquals(2, result.getTeams().size());
    assertEquals(List.of(), result.getUnmatchedPlayers());
  }

  @Test
  void assignsHosterWhenHosterSignedUpEvenIfThatCreatesFewerTeams() {
    EventRole hosterRole = role(1L, "Hoster Role");
    EventRole firstSupportRole = role(2L, "First Support");
    EventRole secondSupportRole = role(3L, "Second Support");
    EventRole north = role(4L, "North");
    EventRole east = role(5L, "East");
    StrategyTemplate hosterStrategy = new StrategyTemplate(1L, "Hoster Strategy");
    hosterStrategy.addRole(hosterRole);
    hosterStrategy.addRole(firstSupportRole);
    hosterStrategy.addRole(secondSupportRole);
    StrategyTemplate regularStrategy = new StrategyTemplate(2L, "Regular Strategy");
    regularStrategy.addRole(north);
    regularStrategy.addRole(east);
    EventTemplate template = new EventTemplate(1L, "Raid");
    template.addStrategy(hosterStrategy);
    template.addStrategy(regularStrategy);
    EventSignup signup = new EventSignup(1L, "guild", "channel", "message", "hoster");
    signup.addTemplate(template);
    PlayerSignup hoster = new PlayerSignup("hoster", List.of(hosterRole), List.of());
    PlayerSignup firstSupport = new PlayerSignup("first-support", List.of(firstSupportRole, east), List.of());
    PlayerSignup secondSupport = new PlayerSignup("second-support", List.of(secondSupportRole, east), List.of());
    signup.addOrUpdatePlayerSignup(hoster);
    signup.addOrUpdatePlayerSignup(firstSupport);
    signup.addOrUpdatePlayerSignup(secondSupport);
    signup.addOrUpdatePlayerSignup(new PlayerSignup("north-1", List.of(north), List.of()));
    signup.addOrUpdatePlayerSignup(new PlayerSignup("north-2", List.of(north), List.of()));

    TeamGeneratorResult result = service.generateTeams(signup);

    assertEquals(1, result.getTeams().size());
    assertEquals("Hoster Strategy", result.getTeams().getFirst().getStrategy().getName());
    assertTrue(result.getMatchedPlayers().contains(hoster));
  }

  @Test
  void doesNotSacrificeTeamsForHosterWhenHosterDidNotSignUp() {
    EventRole hosterRole = role(1L, "Hoster Role");
    EventRole firstSupportRole = role(2L, "First Support");
    EventRole secondSupportRole = role(3L, "Second Support");
    EventRole north = role(4L, "North");
    EventRole east = role(5L, "East");
    StrategyTemplate hosterStrategy = new StrategyTemplate(1L, "Hoster Strategy");
    hosterStrategy.addRole(hosterRole);
    hosterStrategy.addRole(firstSupportRole);
    hosterStrategy.addRole(secondSupportRole);
    StrategyTemplate regularStrategy = new StrategyTemplate(2L, "Regular Strategy");
    regularStrategy.addRole(north);
    regularStrategy.addRole(east);
    EventTemplate template = new EventTemplate(1L, "Raid");
    template.addStrategy(hosterStrategy);
    template.addStrategy(regularStrategy);
    EventSignup signup = new EventSignup(1L, "guild", "channel", "message", "hoster");
    signup.addTemplate(template);
    signup.addOrUpdatePlayerSignup(new PlayerSignup("first-support", List.of(firstSupportRole, east), List.of()));
    signup.addOrUpdatePlayerSignup(new PlayerSignup("second-support", List.of(secondSupportRole, east), List.of()));
    signup.addOrUpdatePlayerSignup(new PlayerSignup("north-1", List.of(north), List.of()));
    signup.addOrUpdatePlayerSignup(new PlayerSignup("north-2", List.of(north), List.of()));

    TeamGeneratorResult result = service.generateTeams(signup);

    assertEquals(2, result.getTeams().size());
    assertEquals(Set.of("Regular Strategy"), result.getTeams().stream()
        .map(team -> team.getStrategy().getName())
        .collect(Collectors.toSet()));
  }

  @Test
  void createsNoTeamsWhenSignedUpHosterCannotBeAssignedToAnyValidTeam() {
    EventRole hosterRole = role(1L, "Hoster Role");
    EventRole north = role(2L, "North");
    EventRole east = role(3L, "East");
    StrategyTemplate regularStrategy = new StrategyTemplate(1L, "Regular Strategy");
    regularStrategy.addRole(north);
    regularStrategy.addRole(east);
    EventTemplate template = new EventTemplate(1L, "Raid");
    template.addStrategy(regularStrategy);
    PlayerSignup hoster = new PlayerSignup("hoster", List.of(hosterRole), List.of());
    PlayerSignup northPlayer = new PlayerSignup("north", List.of(north), List.of());
    PlayerSignup eastPlayer = new PlayerSignup("east", List.of(east), List.of());
    EventSignup signup = new EventSignup(1L, "guild", "channel", "message", "hoster");
    signup.addTemplate(template);
    signup.addOrUpdatePlayerSignup(hoster);
    signup.addOrUpdatePlayerSignup(northPlayer);
    signup.addOrUpdatePlayerSignup(eastPlayer);

    TeamGeneratorResult result = service.generateTeams(signup);

    assertEquals(0, result.getTeams().size());
    assertEquals(List.of(hoster, northPlayer, eastPlayer), result.getUnmatchedPlayers());
  }

  @Test
  void priorityDoesNotReduceTeamCountForNonHosterPlayers() {
    EventRole priorityRole = role(1L, "Priority Role");
    EventRole north = role(2L, "North");
    EventRole east = role(3L, "East");
    StrategyTemplate priorityStrategy = new StrategyTemplate(1L, "Priority Strategy");
    priorityStrategy.addRole(priorityRole);
    priorityStrategy.addRole(east);
    priorityStrategy.addRole(east);
    StrategyTemplate regularStrategy = new StrategyTemplate(2L, "Regular Strategy");
    regularStrategy.addRole(north);
    regularStrategy.addRole(east);
    EventTemplate template = new EventTemplate(1L, "Raid");
    template.addStrategy(priorityStrategy);
    template.addStrategy(regularStrategy);
    PlayerProfile priorityProfile = new PlayerProfile("priority");
    priorityProfile.setPriorityScore(100);
    PlayerSignup priorityPlayer = new PlayerSignup(priorityProfile, List.of(priorityRole), List.of());
    EventSignup signup = new EventSignup(1L, "guild", "channel", "message");
    signup.addTemplate(template);
    signup.addOrUpdatePlayerSignup(priorityPlayer);
    signup.addOrUpdatePlayerSignup(new PlayerSignup("north-1", List.of(north), List.of()));
    signup.addOrUpdatePlayerSignup(new PlayerSignup("east-1", List.of(east), List.of()));
    signup.addOrUpdatePlayerSignup(new PlayerSignup("north-2", List.of(north), List.of()));
    signup.addOrUpdatePlayerSignup(new PlayerSignup("east-2", List.of(east), List.of()));

    TeamGeneratorResult result = service.generateTeams(signup);

    assertEquals(2, result.getTeams().size());
    assertTrue(result.getUnmatchedPlayers().contains(priorityPlayer));
  }

  @Test
  void supportsDuplicateRequiredRolesInStrategy() {
    EventRole stormSlot = role(1L, "Storm");
    StrategyTemplate strategy = new StrategyTemplate(1L, "Double Storm");
    strategy.addRole(stormSlot);
    strategy.addRole(stormSlot);
    EventTemplate template = new EventTemplate(1L, "Raid");
    template.addStrategy(strategy);
    PlayerSignup firstStorm = new PlayerSignup("storm-1", List.of(stormSlot), List.of());
    PlayerSignup secondStorm = new PlayerSignup("storm-2", List.of(stormSlot), List.of());
    EventSignup signup = new EventSignup(1L, "guild", "channel", "message");
    signup.addTemplate(template);
    signup.addOrUpdatePlayerSignup(firstStorm);
    signup.addOrUpdatePlayerSignup(secondStorm);

    TeamGeneratorResult result = service.generateTeams(signup);

    assertEquals(1, result.getTeams().size());
    assertEquals(2, result.getTeams().getFirst().getSlotAssignments().size());
    assertTrue(result.getUnmatchedPlayers().isEmpty());
  }

  @Test
  void assignsRequiredUtilityFromEligibleAssignedPlayer() {
    EventRole stormSlot = role(1L, "Storm");
    EventRole mythSlot = role(2L, "Myth");
    EventUtilityRole wcHex = utilityRole(1L, "WC Hex", 1);
    wcHex.addEligibleRole(stormSlot);
    StrategyTemplate strategy = new StrategyTemplate(1L, "Storm Strategy");
    strategy.addRole(stormSlot);
    strategy.addRole(mythSlot);
    strategy.addUtilityRole(wcHex);
    EventTemplate template = new EventTemplate(1L, "Raid");
    template.addStrategy(strategy);
    PlayerSignup stormPlayer = new PlayerSignup("storm-player");
    stormPlayer.addSelectedRole(stormSlot);
    stormPlayer.addSelectedUtility(wcHex);
    PlayerSignup mythPlayer = new PlayerSignup("myth-player");
    mythPlayer.addSelectedRole(mythSlot);
    EventSignup signup = new EventSignup(1L, "guild", "channel", "message");
    signup.addTemplate(template);
    signup.addOrUpdatePlayerSignup(stormPlayer);
    signup.addOrUpdatePlayerSignup(mythPlayer);

    TeamGeneratorResult result = service.generateTeams(signup);

    assertEquals(1, result.getTeams().size());
    assertEquals(1, result.getTeams().getFirst().getUtilityAssignments().size());
    assertEquals(stormPlayer, result.getTeams().getFirst().getUtilityAssignments().getFirst().playerSignup());
  }

  @Test
  void assignsMultipleRequiredUtilitiesFromDifferentEligibleAssignedPlayers() {
    EventRole stormSlot = role(1L, "Storm");
    EventRole mythSlot = role(2L, "Myth");
    EventRole fireSlot = role(3L, "Fire");
    EventUtilityRole wcHex = utilityRole(1L, "WC Hex", 2);
    wcHex.addEligibleRole(stormSlot);
    wcHex.addEligibleRole(fireSlot);
    StrategyTemplate strategy = new StrategyTemplate(1L, "Utility Strategy");
    strategy.addRole(stormSlot);
    strategy.addRole(mythSlot);
    strategy.addRole(fireSlot);
    strategy.addUtilityRole(wcHex);
    EventTemplate template = new EventTemplate(1L, "Raid");
    template.addStrategy(strategy);
    PlayerSignup stormPlayer = new PlayerSignup("storm-player", List.of(stormSlot), List.of(wcHex));
    PlayerSignup mythPlayer = new PlayerSignup("myth-player", List.of(mythSlot), List.of(wcHex));
    PlayerSignup firePlayer = new PlayerSignup("fire-player", List.of(fireSlot), List.of(wcHex));
    EventSignup signup = new EventSignup(1L, "guild", "channel", "message");
    signup.addTemplate(template);
    signup.addOrUpdatePlayerSignup(stormPlayer);
    signup.addOrUpdatePlayerSignup(mythPlayer);
    signup.addOrUpdatePlayerSignup(firePlayer);

    TeamGeneratorResult result = service.generateTeams(signup);

    assertEquals(1, result.getTeams().size());
    assertEquals(2, result.getTeams().getFirst().getUtilityAssignments().size());
    assertEquals(
        Set.of(stormPlayer, firePlayer),
        result.getTeams().getFirst().getUtilityAssignments().stream()
            .map(UtilityAssignment::playerSignup)
            .collect(Collectors.toSet())
    );
  }

  @Test
  void doesNotCreateTeamWhenRequiredUtilityIsMissing() {
    EventRole stormSlot = role(1L, "Storm");
    EventRole mythSlot = role(2L, "Myth");
    EventUtilityRole wcHex = utilityRole(1L, "WC Hex", 1);
    wcHex.addEligibleRole(stormSlot);
    StrategyTemplate strategy = new StrategyTemplate(1L, "Storm Strategy");
    strategy.addRole(stormSlot);
    strategy.addRole(mythSlot);
    strategy.addUtilityRole(wcHex);
    EventTemplate template = new EventTemplate(1L, "Raid");
    template.addStrategy(strategy);
    PlayerSignup stormPlayerWithoutUtility = new PlayerSignup("storm-player");
    stormPlayerWithoutUtility.addSelectedRole(stormSlot);
    PlayerSignup mythPlayerWithUtility = new PlayerSignup("myth-player");
    mythPlayerWithUtility.addSelectedRole(mythSlot);
    mythPlayerWithUtility.addSelectedUtility(wcHex);
    EventSignup signup = new EventSignup(1L, "guild", "channel", "message");
    signup.addTemplate(template);
    signup.addOrUpdatePlayerSignup(stormPlayerWithoutUtility);
    signup.addOrUpdatePlayerSignup(mythPlayerWithUtility);

    TeamGeneratorResult result = service.generateTeams(signup);

    assertEquals(0, result.getTeams().size());
    assertEquals(List.of(stormPlayerWithoutUtility, mythPlayerWithUtility), result.getUnmatchedPlayers());
  }

  @Test
  void doesNotUseUnassignedPlayersForRequiredUtility() {
    EventRole stormSlot = role(1L, "Storm");
    EventRole mythSlot = role(2L, "Myth");
    EventUtilityRole wcHex = utilityRole(1L, "WC Hex", 1);
    wcHex.addEligibleRole(stormSlot);
    StrategyTemplate strategy = new StrategyTemplate(1L, "Storm Strategy");
    strategy.addRole(stormSlot);
    strategy.addRole(mythSlot);
    strategy.addUtilityRole(wcHex);
    EventTemplate template = new EventTemplate(1L, "Raid");
    template.addStrategy(strategy);
    PlayerSignup assignedStormWithoutUtility = new PlayerSignup("storm-player", List.of(stormSlot), List.of());
    PlayerSignup assignedMyth = new PlayerSignup("myth-player", List.of(mythSlot), List.of());
    PlayerSignup unassignedUtilityProvider = new PlayerSignup("utility-only", List.of(), List.of(wcHex));
    EventSignup signup = new EventSignup(1L, "guild", "channel", "message");
    signup.addTemplate(template);
    signup.addOrUpdatePlayerSignup(assignedStormWithoutUtility);
    signup.addOrUpdatePlayerSignup(assignedMyth);
    signup.addOrUpdatePlayerSignup(unassignedUtilityProvider);

    TeamGeneratorResult result = service.generateTeams(signup);

    assertEquals(0, result.getTeams().size());
    assertEquals(
        List.of(assignedStormWithoutUtility, assignedMyth, unassignedUtilityProvider),
        result.getUnmatchedPlayers()
    );
  }

  @Test
  void canMixStrategiesAcrossGeneratedTeams() {
    EventRole north = role(1L, "North");
    EventRole east = role(2L, "East");
    EventRole storm = role(3L, "Storm");
    EventRole myth = role(4L, "Myth");

    StrategyTemplate outsideStrategy = new StrategyTemplate(1L, "Outside");
    outsideStrategy.addRole(north);
    outsideStrategy.addRole(east);

    StrategyTemplate insideStrategy = new StrategyTemplate(2L, "Inside");
    insideStrategy.addRole(storm);
    insideStrategy.addRole(myth);

    EventTemplate template = new EventTemplate(1L, "Raid");
    template.addStrategy(outsideStrategy);
    template.addStrategy(insideStrategy);

    EventSignup signup = new EventSignup(1L, "guild", "channel", "message");
    signup.addTemplate(template);
    signup.addOrUpdatePlayerSignup(new PlayerSignup("outside-1", List.of(north), List.of()));
    signup.addOrUpdatePlayerSignup(new PlayerSignup("outside-2", List.of(east), List.of()));
    signup.addOrUpdatePlayerSignup(new PlayerSignup("inside-1", List.of(storm), List.of()));
    signup.addOrUpdatePlayerSignup(new PlayerSignup("inside-2", List.of(myth), List.of()));

    TeamGeneratorResult result = service.generateTeams(signup);

    assertEquals(2, result.getTeams().size());
    assertEquals(Set.of("Outside", "Inside"), Set.of(
        result.getTeams().get(0).getStrategy().getName(),
        result.getTeams().get(1).getStrategy().getName()
    ));
    assertTrue(result.getUnmatchedPlayers().isEmpty());
  }

  @Test
  void neverAssignsTheSamePlayerTwiceInOneTeam() {
    EventRole north = role(1L, "North");
    EventRole east = role(2L, "East");
    StrategyTemplate strategy = new StrategyTemplate(1L, "Default");
    strategy.addRole(north);
    strategy.addRole(east);
    EventTemplate template = new EventTemplate(1L, "Raid");
    template.addStrategy(strategy);
    PlayerSignup flexiblePlayer = new PlayerSignup("flexible", List.of(north, east), List.of());
    EventSignup signup = new EventSignup(1L, "guild", "channel", "message");
    signup.addTemplate(template);
    signup.addOrUpdatePlayerSignup(flexiblePlayer);

    TeamGeneratorResult result = service.generateTeams(signup);

    assertEquals(0, result.getTeams().size());
    assertEquals(List.of(flexiblePlayer), result.getUnmatchedPlayers());
  }

  private static EventRole role(Long id, String name) {
    EventRole role = new EventRole(name);
    role.setId(id);
    return role;
  }

  private static EventUtilityRole utilityRole(Long id, String name, int requiredCount) {
    EventUtilityRole utilityRole = new EventUtilityRole(name, requiredCount);
    utilityRole.setId(id);
    return utilityRole;
  }
}
