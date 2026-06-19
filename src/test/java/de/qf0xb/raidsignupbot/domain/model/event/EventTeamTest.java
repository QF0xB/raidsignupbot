package de.qf0xb.raidsignupbot.domain.model.event;

import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventTeamTest {
  @Test
  void createsSlotsFromRequiredCounts() {
    EventStrategy strategy = new EventStrategy("strat", "Strategy");
    strategy.addRole(new EventRole("tank", "Tank", "tank", 2));
    EventUtilityRole flex = new EventUtilityRole("flex", "Flex", "flex", 1);
    flex.addEligiblePrimaryRole(new EventRole("tank", "Tank", "tank", 2));
    strategy.addRole(flex);

    EventTeam team = new EventTeam(strategy);

    assertEquals(2, team.getSlots().size());
    assertEquals(1, team.getUtilityAssignments().size());
    assertEquals(2, team.freeSlotsFor("tank").size());
    assertEquals(0, team.freeSlotsFor("flex").size());
    assertFalse(team.getSlots().get(0).isUtility());
  }

  @Test
  void assignsFirstFreeMatchingSlot() {
    EventStrategy strategy = new EventStrategy("strat", "Strategy");
    strategy.addRole(new EventRole("tank", "Tank", "tank", 2));
    EventTeam team = new EventTeam(strategy);
    GuildMember member = new GuildMember("user-1");

    team.assign("tank", member);

    assertTrue(team.getSlots().get(0).isAssigned());
    assertSame(member, team.getSlots().get(0).getMember());
    assertFalse(team.getSlots().get(1).isAssigned());
  }

  @Test
  void unassignClearsSlotWithoutRemovingIt() {
    EventStrategy strategy = new EventStrategy("strat", "Strategy");
    strategy.addRole(new EventRole("tank", "Tank", "tank", 1));
    EventTeam team = new EventTeam(strategy);
    GuildMember member = new GuildMember("user-1");
    EventSlot slot = team.getSlots().get(0);

    team.assign("tank", member);
    team.unassign(slot);

    assertFalse(slot.isAssigned());
    assertEquals(1, team.getSlots().size());
  }

  @Test
  void failsWhenNoMatchingSlotExists() {
    EventStrategy strategy = new EventStrategy("strat", "Strategy");
    strategy.addRole(new EventRole("tank", "Tank", "tank", 1));
    EventTeam team = new EventTeam(strategy);

    assertThrows(IllegalStateException.class, () -> team.assign("healer", new GuildMember("user-1")));
  }

  @Test
  void utilityCanOnlyBeAssignedToEligibleSeatedMember() {
    EventStrategy strategy = new EventStrategy("strat", "Strategy");
    EventRole tank = new EventRole("tank", "Tank", "tank", 1);
    strategy.addRole(tank);
    EventUtilityRole flex = new EventUtilityRole("flex", "Flex", "flex", 1);
    flex.addEligiblePrimaryRole(tank);
    strategy.addRole(flex);
    EventTeam team = new EventTeam(strategy);
    GuildMember member = new GuildMember("user-1");

    assertThrows(IllegalStateException.class, () -> team.assignUtility("flex", member));

    team.assign("tank", member);
    team.assignUtility("flex", member);

    assertTrue(team.getUtilityAssignments().getFirst().isAssigned());
    assertSame(member, team.getUtilityAssignments().getFirst().getMember());
    assertTrue(team.isFilled());
  }

  @Test
  void unassigningPrimarySlotClearsUtilityAssignmentsForThatMember() {
    EventStrategy strategy = new EventStrategy("strat", "Strategy");
    EventRole tank = new EventRole("tank", "Tank", "tank", 1);
    strategy.addRole(tank);
    EventUtilityRole flex = new EventUtilityRole("flex", "Flex", "flex", 1);
    flex.addEligiblePrimaryRole(tank);
    strategy.addRole(flex);
    EventTeam team = new EventTeam(strategy);
    GuildMember member = new GuildMember("user-1");
    EventSlot slot = team.getSlots().getFirst();

    team.assign("tank", member);
    team.assignUtility("flex", member);
    team.unassign(slot);

    assertFalse(slot.isAssigned());
    assertFalse(team.getUtilityAssignments().getFirst().isAssigned());
    assertFalse(team.isFilled());
  }
}
