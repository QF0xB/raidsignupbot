package de.qf0xb.raidsignupbot.application.service;

import de.qf0xb.raidsignupbot.application.event.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.application.event.service.GreedyTeamGeneratorService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class GreedyTeamGeneratorServiceTest {
  private final GreedyTeamGeneratorService service = new GreedyTeamGeneratorService();

  @Test
  void buildsACompleteTeamForSimpleInput() {
    EventTemplate template = new EventTemplate("temp", "Temp", "Details");
    EventStrategy strategy = new EventStrategy("strat", "Strategy");
    strategy.addRole(new EventRole("tank", "Tank", "shield", 1));
    strategy.addRole(new EventRole("healer", "Healer", "plus", 1));
    template.addStrategy(strategy);

    Event event = Event.builder()
        .id(1L)
        .guild(new Guild("guild-1", "Guild", "raider"))
        .hoster(new GuildMember("hoster"))
        .template(template)
        .build();
    List<EventMemberSignup> signups = List.of(
        signup("a", "tank"),
        signup("b", "healer")
    );

    TeamGeneratorResult result = service.generateTeams(event, signups);

    assertEquals(1, result.teams().size());
    assertTrue(result.unmatchedSignups().isEmpty());
    EventTeam team = result.teams().getFirst();
    assertEquals(2, team.getSlots().size());
    assertTrue(team.isFilled());
  }

  @Test
  void utilityIsAssignedToExistingPrimaryMember() {
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
        signup("a", "tank"),
        signup("b", "tank", "flex")
    );

    TeamGeneratorResult result = service.generateTeams(event, signups);

    assertEquals(1, result.teams().size());
    assertEquals(1, result.unmatchedSignups().size());
    EventTeam team = result.teams().getFirst();
    assertEquals(1, team.getSlots().size());
    assertEquals(1, team.getUtilityAssignments().size());
    assertTrue(team.isFilled());
    assertEquals("b", team.getSlots().getFirst().getMember().getUserId());
    assertEquals("b", team.getUtilityAssignments().getFirst().getMember().getUserId());
  }

  private EventMemberSignup signup(String userId, String... roleKeys) {
    EventMemberSignup signup = new EventMemberSignup(new GuildMember(userId));
    for (String roleKey : roleKeys) {
      signup.addRoleKey(roleKey);
    }
    return signup;
  }
}
