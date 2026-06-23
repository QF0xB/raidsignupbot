package de.qf0xb.raidsignupbot.application.service;

import de.qf0xb.raidsignupbot.application.event.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.application.event.service.OrToolsTeamGeneratorService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrToolsTeamGeneratorServiceTest {
  private final OrToolsTeamGeneratorService service = new OrToolsTeamGeneratorService();

  @Test
  void buildsACompleteTeamFromPrimaryAndUtilitySignups() {
    EventTemplate template = new EventTemplate("temp", "Temp", "Details");
    EventStrategy strategy = new EventStrategy("strat", "Strategy");
    EventRole tank = new EventRole("tank", "Tank", "shield", 1);
    EventUtilityRole flex = new EventUtilityRole("flex", "Flex", "wrench", 1);
    flex.addEligiblePrimaryRole(tank);
    strategy.addRole(tank);
    strategy.addRole(flex);
    template.addStrategy(strategy);

    Event event = Event.builder()
        .id(1L)
        .guild(new Guild("guild-1", "Guild", "raider"))
        .hoster(new GuildMember("hoster"))
        .template(template)
        .build();

    List<EventMemberSignup> signups = List.of(
        signup("a-tank", "tank", "flex"),
        signup("b-flex", "flex")
    );

    TeamGeneratorResult result = service.generateTeams(event, signups);

    assertEquals(1, result.teams().size());
    assertEquals(1, result.unmatchedSignups().size());

    EventTeam team = result.teams().getFirst();
    assertEquals(1, team.getSlots().size());
    assertEquals(1, team.getUtilityAssignments().size());
    assertTrue(team.isFilled());
    assertEquals("a-tank", team.getSlots().getFirst().getMember().getUserId());
    assertEquals("a-tank", team.getUtilityAssignments().getFirst().getMember().getUserId());
  }

  @Test
  void buildsFourTeamsForMixedStormAndMythSignups() {
    EventTemplate template = new EventTemplate("takanobu", "Takanobu", "Takanobu details");
    EventStrategy storm = new EventStrategy("storm", "Storm Strategy");
    storm.addRole(new EventRole("one", "Potent Feint", ":one:", 1));
    storm.addRole(new EventRole("two", "Sharpened Elemental Blade", ":two:", 1));
    storm.addRole(new EventRole("three", "IC Sharpened Elemental Blade", ":three:", 1));
    storm.addRole(new EventRole("four", "Epic Bunyip", ":four:", 1));
    template.addStrategy(storm);

    EventStrategy myth = new EventStrategy("myth", "Myth Strategy");
    myth.addRole(new EventRole("a", "Potent Feint", ":regional_indicator_a:", 1));
    myth.addRole(new EventRole("b", "Sharpened Spirit Blade", ":regional_indicator_b:", 1));
    myth.addRole(new EventRole("c", "IC Sharpened Spirit Blade", ":regional_indicator_c:", 1));
    myth.addRole(new EventRole("d", "Epic Drop Bear", ":regional_indicator_d:", 1));
    EventUtilityRole hex = new EventUtilityRole("hex", "Hex", ":schoolSun-1:", 1);
    hex.addEligiblePrimaryRole("a");
    hex.addEligiblePrimaryRole("b");
    hex.addEligiblePrimaryRole("c");
    myth.addRole(hex);
    template.addStrategy(myth);

    Event event = Event.builder()
        .id(1L)
        .guild(new Guild("guild-1", "Guild", "raider"))
        .hoster(new GuildMember("hoster"))
        .template(template)
        .build();

    List<EventMemberSignup> signups = List.of(
        signup("aidan", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup("iceberg", "one", "two", "three", "a", "b", "c"),
        signup("goober", "one", "two", "three", "a"),
        signup("chase", "one", "two", "three", "four"),
        signup("aria", "one", "two", "three", "a", "hex"),
        signup("ethan", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup("sia", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup("TypeX", "one", "two", "three"),
        signup("Xenia", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup("Voli", "one", "two", "three", "a", "b", "c", "hex"),
        signup("OsaRion", "one", "two", "a"),
        signup("Tara", "one", "two", "three", "a", "b", "c"),
        signup("Elijah", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup("Blue", "one", "two", "three", "four", "a", "b", "c", "hex"),
        signup("vinzrub", "one", "two", "three", "a", "b", "c"),
        signup("maya", "one", "two", "three", "a", "b", "c"),
        signup("angel", "one", "two", "four", "a", "b")
    );

    TeamGeneratorResult result = service.generateTeams(event, signups);

    assertEquals(4, result.teams().size());
    assertEquals(1, result.unmatchedSignups().size());
    assertTrue(result.teams().stream().allMatch(EventTeam::isFilled));
    assertTrue(result.teams().stream().allMatch(team -> team.getSlots().size() == 4));
    assertTrue(result.teams().stream()
        .flatMap(team -> team.getUtilityAssignments().stream())
        .allMatch(assignment -> assignment.getMember() != null));
  }

  @Test
  void doesNotDropValidTeamsWhenHosterCannotBePlaced() {
    EventTemplate template = new EventTemplate("temp", "Temp", "Details");
    EventStrategy strategy = new EventStrategy("strat", "Strategy");
    strategy.addRole(new EventRole("tank", "Tank", "shield", 1));
    template.addStrategy(strategy);

    GuildMember hoster = new GuildMember("hoster");
    Event event = Event.builder()
        .id(1L)
        .guild(new Guild("guild-1", "Guild", "raider"))
        .hoster(hoster)
        .template(template)
        .build();

    List<EventMemberSignup> signups = List.of(
        signup(hoster, "healer"),
        signup("a-tank", "tank")
    );

    TeamGeneratorResult result = service.generateTeams(event, signups);

    assertEquals(1, result.teams().size());
    assertEquals(1, result.unmatchedSignups().size());
    assertTrue(result.teams().getFirst().isFilled());
    assertEquals("a-tank", result.teams().getFirst().getSlots().getFirst().getMember().getUserId());
  }

  @Test
  void rejectsTeamsThatCannotCoverRequiredUtility() {
    EventTemplate template = new EventTemplate("temp", "Temp", "Details");
    EventStrategy strategy = new EventStrategy("strat", "Strategy");
    strategy.addRole(new EventRole("tank", "Tank", "shield", 1));
    EventUtilityRole hex = new EventUtilityRole("hex", "Hex", ":schoolSun-1:", 1);
    hex.addEligiblePrimaryRole("tank");
    strategy.addRole(hex);
    template.addStrategy(strategy);

    Event event = Event.builder()
        .id(1L)
        .guild(new Guild("guild-1", "Guild", "raider"))
        .hoster(new GuildMember("hoster"))
        .template(template)
        .build();

    List<EventMemberSignup> signups = List.of(
        signup("a-tank", "tank")
    );

    TeamGeneratorResult result = service.generateTeams(event, signups);

    assertTrue(result.teams().isEmpty());
    assertEquals(1, result.unmatchedSignups().size());
  }

  @Test
  void generatesLargeMixedRaidWithUtilities() {
    EventTemplate template = new EventTemplate("large-test", "Large Test Raid", "Stress test");

    EventStrategy storm = new EventStrategy("storm", "Storm Strategy");
    storm.addRole(new EventRole("one", "Potent Feint", ":one:", 1));
    storm.addRole(new EventRole("two", "Sharpened Elemental Blade", ":two:", 1));
    storm.addRole(new EventRole("three", "IC Sharpened Elemental Blade", ":three:", 1));
    storm.addRole(new EventRole("four", "Epic Bunyip", ":four:", 1));
    template.addStrategy(storm);

    EventStrategy myth = new EventStrategy("myth", "Myth Strategy");
    myth.addRole(new EventRole("a", "Potent Feint", ":regional_indicator_a:", 1));
    myth.addRole(new EventRole("b", "Sharpened Spirit Blade", ":regional_indicator_b:", 1));
    myth.addRole(new EventRole("c", "IC Sharpened Spirit Blade", ":regional_indicator_c:", 1));
    myth.addRole(new EventRole("d", "Epic Drop Bear", ":regional_indicator_d:", 1));

    EventUtilityRole hex = new EventUtilityRole("hex", "Hex", ":schoolSun-1:", 1);
    hex.addEligiblePrimaryRole("a");
    hex.addEligiblePrimaryRole("b");
    hex.addEligiblePrimaryRole("c");
    myth.addRole(hex);
    template.addStrategy(myth);

    GuildMember hoster = new GuildMember("owner");

    Event event = Event.builder()
        .id(null)
        .start(Instant.parse("2026-01-01T18:00:00Z"))
        .end(Instant.parse("2026-01-01T19:00:00Z"))
        .hoster(hoster)
        .template(template)
        .build();

    List<EventMemberSignup> signups = List.of(
        // Owner should be selected if possible.
        new EventMemberSignup(hoster, "one", "two", "three", "four", "a", "b", "c", "d", "hex"),

        // Storm-only constrained players.
        new EventMemberSignup(new GuildMember("storm_four_01"), "four"),
        new EventMemberSignup(new GuildMember("storm_four_02"), "four"),
        new EventMemberSignup(new GuildMember("storm_four_03"), "four"),
        new EventMemberSignup(new GuildMember("storm_one_01"), "one"),
        new EventMemberSignup(new GuildMember("storm_two_01"), "two"),
        new EventMemberSignup(new GuildMember("storm_three_01"), "three"),

        // Myth-only constrained players.
        new EventMemberSignup(new GuildMember("myth_d_01"), "d"),
        new EventMemberSignup(new GuildMember("myth_d_02"), "d"),
        new EventMemberSignup(new GuildMember("myth_d_03"), "d"),
        new EventMemberSignup(new GuildMember("myth_a_hex_01"), "a", "hex"),
        new EventMemberSignup(new GuildMember("myth_b_hex_01"), "b", "hex"),
        new EventMemberSignup(new GuildMember("myth_c_hex_01"), "c", "hex"),

        // Flexible Storm/Myth players.
        new EventMemberSignup(new GuildMember("flex_01"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        new EventMemberSignup(new GuildMember("flex_02"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        new EventMemberSignup(new GuildMember("flex_03"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        new EventMemberSignup(new GuildMember("flex_04"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        new EventMemberSignup(new GuildMember("flex_05"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        new EventMemberSignup(new GuildMember("flex_06"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        new EventMemberSignup(new GuildMember("flex_07"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        new EventMemberSignup(new GuildMember("flex_08"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        new EventMemberSignup(new GuildMember("flex_09"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        new EventMemberSignup(new GuildMember("flex_10"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),

        // Semi-flexible players.
        new EventMemberSignup(new GuildMember("semi_01"), "one", "two", "three", "a", "b", "c"),
        new EventMemberSignup(new GuildMember("semi_02"), "one", "two", "three", "a", "b", "c"),
        new EventMemberSignup(new GuildMember("semi_03"), "one", "two", "three", "a", "b", "c"),
        new EventMemberSignup(new GuildMember("semi_04"), "one", "two", "three", "a", "b", "c"),
        new EventMemberSignup(new GuildMember("semi_05"), "one", "two", "three", "a", "b", "c"),
        new EventMemberSignup(new GuildMember("semi_06"), "one", "two", "three", "a", "b", "c"),

        // Bad utility edge case: has hex but no eligible primary role.
        new EventMemberSignup(new GuildMember("hex_only_invalid"), "hex")
    );

    TeamGeneratorResult result = service.generateTeams(event, signups);

    assertEquals(7, result.teams().size());
    assertEquals(2, result.unmatchedSignups().size());

    assertTrue(
        result.teams().stream()
            .flatMap(team -> team.getSlots().stream())
            .anyMatch(slot -> slot.getMember() != null
                && slot.getMember().getUserId().equals("owner"))
    );

    long mythTeams = result.teams().stream()
        .filter(team -> team.getStrategy().getKey().equals("myth"))
        .count();

    long stormTeams = result.teams().stream()
        .filter(team -> team.getStrategy().getKey().equals("storm"))
        .count();

    assertTrue(mythTeams > 0);
    assertTrue(stormTeams > 0);
  }

  private EventMemberSignup signup(String userId, String... roleKeys) {
    EventMemberSignup signup = new EventMemberSignup(new GuildMember(userId));
    for (String roleKey : roleKeys) {
      signup.addRoleKey(roleKey);
    }
    return signup;
  }

  private EventMemberSignup signup(GuildMember member, String... roleKeys) {
    EventMemberSignup signup = new EventMemberSignup(member);
    for (String roleKey : roleKeys) {
      signup.addRoleKey(roleKey);
    }
    return signup;
  }
}
