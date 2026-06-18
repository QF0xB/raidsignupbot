package de.qf0xb.raidsignupbot.event.domain.service;

import de.qf0xb.raidsignupbot.event.domain.model.Event;
import de.qf0xb.raidsignupbot.event.domain.model.EventMemberSignup;
import de.qf0xb.raidsignupbot.event.domain.model.EventRole;
import de.qf0xb.raidsignupbot.event.domain.model.EventStrategy;
import de.qf0xb.raidsignupbot.event.domain.model.EventTemplate;
import de.qf0xb.raidsignupbot.guild.domain.model.GuildMember;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TeamGeneratorServiceTest {
  private final TeamGeneratorService service = new TeamGeneratorService();

  @Test
  void createsTeamUsingRequiredRoleCounts() {
    EventRole storm = new EventRole("Storm", 2);
    EventStrategy strategy = strategy("double-storm");
    strategy.addRole(storm);
    Event event = eventWith(strategy, null);
    EventMemberSignup firstStorm = signup("storm-1", storm);
    EventMemberSignup secondStorm = signup("storm-2", storm);

    TeamGeneratorResult result = service.generateTeams(event, List.of(firstStorm, secondStorm));

    assertEquals(1, result.getTeams().size());
    assertEquals(2, result.getTeams().getFirst().getAssignments(storm).stream()
        .filter(assignment -> assignment.getMember() != null)
        .count());
    assertTrue(result.getUnmatchedSignups().isEmpty());
  }

  @Test
  void assignsUtilityFromAssignedEligibleMember() {
    EventRole storm = new EventRole("Storm", 1);
    EventRole myth = new EventRole("Myth", 1);
    EventRole hex = new EventRole("Hex", 1, true);
    hex.addEligibleRole(storm);
    EventStrategy strategy = strategy("utility");
    strategy.addRole(storm);
    strategy.addRole(myth);
    strategy.addRole(hex);
    Event event = eventWith(strategy, null);
    EventMemberSignup stormSignup = signup("storm", storm, hex);
    EventMemberSignup mythSignup = signup("myth", myth, hex);

    TeamGeneratorResult result = service.generateTeams(event, List.of(stormSignup, mythSignup));

    assertEquals(1, result.getTeams().size());
    assertEquals("storm", result.getTeams().getFirst().getAssignment(hex).orElseThrow().getMember().getPlayerId());
  }

  @Test
  void doesNotUseUnassignedMemberForUtility() {
    EventRole storm = new EventRole("Storm", 1);
    EventRole myth = new EventRole("Myth", 1);
    EventRole hex = new EventRole("Hex", 1, true);
    hex.addEligibleRole(storm);
    EventStrategy strategy = strategy("utility");
    strategy.addRole(storm);
    strategy.addRole(myth);
    strategy.addRole(hex);
    Event event = eventWith(strategy, null);
    EventMemberSignup stormWithoutHex = signup("storm", storm);
    EventMemberSignup mythSignup = signup("myth", myth);
    EventMemberSignup unassignedHexProvider = signup("hex-only", hex);

    TeamGeneratorResult result = service.generateTeams(event, List.of(stormWithoutHex, mythSignup, unassignedHexProvider));

    assertTrue(result.getTeams().isEmpty());
    assertEquals(3, result.getUnmatchedSignups().size());
  }

  @Test
  void mixesStrategiesToCreateMaximumTeams() {
    EventRole north = new EventRole("North", 1);
    EventRole east = new EventRole("East", 1);
    EventRole storm = new EventRole("Storm", 1);
    EventRole myth = new EventRole("Myth", 1);
    EventStrategy outside = strategy("outside");
    outside.addRole(north);
    outside.addRole(east);
    EventStrategy inside = strategy("inside");
    inside.addRole(storm);
    inside.addRole(myth);
    Event event = eventWith(List.of(outside, inside), null);

    TeamGeneratorResult result = service.generateTeams(event, List.of(
        signup("north", north),
        signup("east", east),
        signup("storm", storm),
        signup("myth", myth)
    ));

    assertEquals(2, result.getTeams().size());
    assertEquals(Set.of("outside", "inside"), result.getTeams().stream()
        .map(team -> team.getStrategy().getSlug())
        .collect(Collectors.toSet()));
  }

  @Test
  void includesHosterEvenWhenThatCreatesFewerTeams() {
    EventRole hosterRole = new EventRole("Hoster", 1);
    EventRole support = new EventRole("Support", 2);
    EventRole solo = new EventRole("Solo", 1);
    EventStrategy hosterStrategy = strategy("hoster-strategy");
    hosterStrategy.addRole(hosterRole);
    hosterStrategy.addRole(support);
    EventStrategy soloStrategy = strategy("solo-strategy");
    soloStrategy.addRole(solo);
    GuildMember hoster = member("hoster");
    Event event = eventWith(List.of(hosterStrategy, soloStrategy), hoster);
    EventMemberSignup hosterSignup = signup(hoster, hosterRole);
    EventMemberSignup firstSupport = signup("support-a", support, solo);
    EventMemberSignup secondSupport = signup("support-b", support, solo);

    TeamGeneratorResult result = service.generateTeams(event, List.of(hosterSignup, firstSupport, secondSupport));

    assertEquals(1, result.getTeams().size());
    assertEquals("hoster-strategy", result.getTeams().getFirst().getStrategy().getSlug());
    assertEquals("hoster", result.getTeams().getFirst().getAssignment(hosterRole).orElseThrow().getMember().getPlayerId());
  }

  private static Event eventWith(EventStrategy strategy, GuildMember hoster) {
    return eventWith(List.of(strategy), hoster);
  }

  private static Event eventWith(List<EventStrategy> strategies, GuildMember hoster) {
    EventTemplate template = new EventTemplate("raid", "Raid", "Raid details");
    strategies.forEach(template::addStrategy);
    return new Event(template, null, hoster, 0, null, null);
  }

  private static EventStrategy strategy(String slug) {
    return new EventStrategy(slug, slug);
  }

  private static EventMemberSignup signup(String playerId, EventRole... roles) {
    return signup(member(playerId), roles);
  }

  private static EventMemberSignup signup(GuildMember member, EventRole... roles) {
    EventMemberSignup signup = new EventMemberSignup(member);
    for (EventRole role : roles) {
      signup.addRole(role);
    }
    return signup;
  }

  private static GuildMember member(String playerId) {
    return new GuildMember(playerId, 0);
  }
}
