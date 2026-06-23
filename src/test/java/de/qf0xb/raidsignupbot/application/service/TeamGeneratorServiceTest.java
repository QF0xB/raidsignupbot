package de.qf0xb.raidsignupbot.application.service;

import de.qf0xb.raidsignupbot.application.event.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.application.event.service.ExactTeamGeneratorService;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamGeneratorServiceTest {
  private final ExactTeamGeneratorService service = new ExactTeamGeneratorService();

  @Test
  void returnsNoTeamsWhenTemplateIsMissing() {
    Event event = Event.builder()
        .id(1L)
        .guild(new Guild("guild-1", "Guild", "raider"))
        .hoster(new GuildMember("hoster"))
        .template(null)
        .build();
    List<EventMemberSignup> signups = List.of(signup("a", "tank"));

    TeamGeneratorResult result = service.generateTeams(event, signups);

    assertTrue(result.teams().isEmpty());
    assertEquals(1, result.unmatchedSignups().size());
    assertSame(signups.getFirst(), result.unmatchedSignups().getFirst());
  }

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
    GuildMember tankMember = new GuildMember("a-tank");
    GuildMember flexOnlyMember = new GuildMember("b-flex");
    List<EventMemberSignup> signups = List.of(
        signup(tankMember, "tank", "flex"),
        signup(flexOnlyMember, "flex")
    );

    TeamGeneratorResult result = service.generateTeams(event, signups);

    assertEquals(1, result.teams().size());
    assertEquals(1, result.unmatchedSignups().size());
    assertTrue(sameMember(flexOnlyMember, result.unmatchedSignups().getFirst().getGuildMember()));

    EventTeam team = result.teams().getFirst();
    assertEquals(1, team.getSlots().size());
    assertEquals(1, team.getUtilityAssignments().size());
    assertTrue(team.isFilled());
    assertTrue(team.getSlots().stream().anyMatch(slot -> slot.getKey().equals("tank") && sameMember(slot.getMember(), tankMember)));
    assertTrue(team.getUtilityAssignments().stream().anyMatch(assignment ->
        assignment.getKey().equals("flex") && sameMember(assignment.getMember(), tankMember)));
  }

  @Test
  void returnsNoTeamsWhenHosterSignsUpButCannotBePlaced() {
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

    assertTrue(result.teams().isEmpty());
    assertEquals(2, result.unmatchedSignups().size());
    assertFalse(result.unmatchedSignups().isEmpty());
  }

  private EventMemberSignup signup(String userId, String... roleKeys) {
    return signup(new GuildMember(userId), roleKeys);
  }

  private EventMemberSignup signup(GuildMember member, String... roleKeys) {
    EventMemberSignup signup = new EventMemberSignup(member);
    for (String roleKey : roleKeys) {
      signup.addRoleKey(roleKey);
    }
    return signup;
  }

  private boolean sameMember(GuildMember left, GuildMember right) {
    return left != null && right != null && left.getUserId().equals(right.getUserId());
  }
}
