package de.qf0xb.raidsignupbot.signup.domain.service;

import de.qf0xb.raidsignupbot.shared.domain.model.StrategySlot;
import de.qf0xb.raidsignupbot.shared.domain.model.StrategyUtility;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamGeneratorServiceTest {
  private final TeamGeneratorService service = new TeamGeneratorService();

  @Test
  void assignsPlayersWithFewestSelectedSlotsFirst() {
    StrategySlot slotA = new StrategySlot(1L, "North", "A");
    StrategySlot slotB = new StrategySlot(2L, "East", "B");
    StrategyTemplate strategy = new StrategyTemplate(1L, "Default");
    strategy.addSlot(slotA);
    strategy.addSlot(slotB);
    EventTemplate template = new EventTemplate(1L, "Raid");
    template.addStrategy(strategy);
    PlayerSignup flexiblePlayer = new PlayerSignup("flexible");
    flexiblePlayer.addSelectedSlot(slotA);
    flexiblePlayer.addSelectedSlot(slotB);
    PlayerSignup onlyA = new PlayerSignup("only-a");
    onlyA.addSelectedSlot(slotA);
    PlayerSignup onlyB = new PlayerSignup("only-b");
    onlyB.addSelectedSlot(slotB);
    EventSignup signup = new EventSignup(1L, "guild", "channel", "message");
    signup.addTemplate(template);
    signup.addOrUpdatePlayerSignup(flexiblePlayer);
    signup.addOrUpdatePlayerSignup(onlyA);
    signup.addOrUpdatePlayerSignup(onlyB);

    TeamGeneratorResult result = service.generateTeams(signup);

    assertEquals(1, result.getTeams().size());
    assertEquals(2, result.getTeams().getFirst().getSlotAssignments().size());
    assertEquals(List.of(flexiblePlayer), result.getUnmatchedPlayers());
  }

  @Test
  void createsMultipleCompleteTeams() {
    StrategySlot slotA = new StrategySlot(1L, "North", "A");
    StrategySlot slotB = new StrategySlot(2L, "East", "B");
    StrategyTemplate strategy = new StrategyTemplate(1L, "Default");
    strategy.addSlot(slotA);
    strategy.addSlot(slotB);
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
  void assignsRequiredUtilityFromEligibleAssignedPlayer() {
    StrategySlot stormSlot = new StrategySlot(1L, "Storm", "1");
    StrategySlot mythSlot = new StrategySlot(2L, "Myth", "2");
    StrategyUtility wcHex = new StrategyUtility(1L, "WC Hex", "sun");
    StrategyUtilityRequirement wcHexRequirement = new StrategyUtilityRequirement(wcHex, 1);
    wcHexRequirement.addEligibleSlot(stormSlot);
    StrategyTemplate strategy = new StrategyTemplate(1L, "Storm Strategy");
    strategy.addSlot(stormSlot);
    strategy.addSlot(mythSlot);
    strategy.addUtilityRequirement(wcHexRequirement);
    EventTemplate template = new EventTemplate(1L, "Raid");
    template.addStrategy(strategy);
    PlayerSignup stormPlayer = new PlayerSignup("storm-player");
    stormPlayer.addSelectedSlot(stormSlot);
    stormPlayer.addSelectedUtility(wcHex);
    PlayerSignup mythPlayer = new PlayerSignup("myth-player");
    mythPlayer.addSelectedSlot(mythSlot);
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
  void doesNotCreateTeamWhenRequiredUtilityIsMissing() {
    StrategySlot stormSlot = new StrategySlot(1L, "Storm", "1");
    StrategySlot mythSlot = new StrategySlot(2L, "Myth", "2");
    StrategyUtility wcHex = new StrategyUtility(1L, "WC Hex", "sun");
    StrategyUtilityRequirement wcHexRequirement = new StrategyUtilityRequirement(wcHex, 1);
    wcHexRequirement.addEligibleSlot(stormSlot);
    StrategyTemplate strategy = new StrategyTemplate(1L, "Storm Strategy");
    strategy.addSlot(stormSlot);
    strategy.addSlot(mythSlot);
    strategy.addUtilityRequirement(wcHexRequirement);
    EventTemplate template = new EventTemplate(1L, "Raid");
    template.addStrategy(strategy);
    PlayerSignup stormPlayerWithoutUtility = new PlayerSignup("storm-player");
    stormPlayerWithoutUtility.addSelectedSlot(stormSlot);
    PlayerSignup mythPlayerWithUtility = new PlayerSignup("myth-player");
    mythPlayerWithUtility.addSelectedSlot(mythSlot);
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
  void canMixStrategiesAcrossGeneratedTeams() {
    StrategySlot north = new StrategySlot(1L, "North", "A");
    StrategySlot east = new StrategySlot(2L, "East", "B");
    StrategySlot storm = new StrategySlot(3L, "Storm", "1");
    StrategySlot myth = new StrategySlot(4L, "Myth", "2");

    StrategyTemplate outsideStrategy = new StrategyTemplate(1L, "Outside");
    outsideStrategy.addSlot(north);
    outsideStrategy.addSlot(east);

    StrategyTemplate insideStrategy = new StrategyTemplate(2L, "Inside");
    insideStrategy.addSlot(storm);
    insideStrategy.addSlot(myth);

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
}
