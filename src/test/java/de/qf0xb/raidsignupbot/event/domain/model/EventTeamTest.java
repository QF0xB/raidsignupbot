package de.qf0xb.raidsignupbot.event.domain.model;

import de.qf0xb.raidsignupbot.guild.domain.model.GuildMember;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventTeamTest {
  @Test
  void createsAssignmentsFromStrategyRoleCounts() {
    EventRole primaryRole = new EventRole("primary", 2);
    EventRole utilityRole = new EventRole("utility", 1, true);
    EventStrategy strategy = new EventStrategy("test", "test");
    strategy.addRole(primaryRole);
    strategy.addRole(utilityRole);

    EventTeam team = new EventTeam(null, strategy);

    assertEquals(3, team.getAssignments().size());
    assertEquals(2, team.getAssignments(primaryRole).size());
    assertEquals(1, team.getAssignments(utilityRole).size());
  }

  @Test
  void assignsMemberToRoleAssignment() {
    EventRole primaryRole = new EventRole("primary", 1);
    EventStrategy strategy = new EventStrategy("test", "test");
    strategy.addRole(primaryRole);
    EventTeam team = new EventTeam(null, strategy);
    GuildMember member = new GuildMember("player", 0);

    team.assignMember(primaryRole, member);

    EventRoleAssignment assignment = team.getAssignment(primaryRole).orElseThrow();
    assertTrue(assignment.isAssigned());
    assertEquals(member, assignment.getMember());
    assertEquals(primaryRole, assignment.getRole());
  }

  @Test
  void assignsMembersToMultipleRequiredSlotsForSameRole() {
    EventRole primaryRole = new EventRole("primary", 2);
    EventStrategy strategy = new EventStrategy("test", "test");
    strategy.addRole(primaryRole);
    EventTeam team = new EventTeam(null, strategy);
    GuildMember firstMember = new GuildMember("first", 0);
    GuildMember secondMember = new GuildMember("second", 0);

    team.assignMember(primaryRole, firstMember);
    team.assignMember(primaryRole, secondMember);

    assertEquals(2, team.getAssignments(primaryRole).stream()
        .filter(EventRoleAssignment::isAssigned)
        .count());
  }

  @Test
  void doesNotCreateAssignmentForRoleThatHasNoFreeSlot() {
    EventRole primaryRole = new EventRole("primary", 1);
    EventStrategy strategy = new EventStrategy("test", "test");
    strategy.addRole(primaryRole);
    EventTeam team = new EventTeam(null, strategy);
    team.assignMember(primaryRole, new GuildMember("first", 0));

    assertThrows(IllegalStateException.class, () ->
        team.assignMember(primaryRole, new GuildMember("second", 0)));
    assertEquals(1, team.getAssignments(primaryRole).size());
  }
}
