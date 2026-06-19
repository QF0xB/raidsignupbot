package de.qf0xb.raidsignupbot.domain.model.template;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventStrategyTest {
  @Test
  void addRoleSeparatesPrimaryAndUtilityRoles() {
    EventStrategy strategy = new EventStrategy("strat", "Strategy");
    EventRole primary = new EventRole("tank", "Tank", "shield", 2);
    EventUtilityRole utility = new EventUtilityRole("flex", "Flex", "wrench", 1);

    strategy.addRole(primary);
    strategy.addRole(utility);

    assertEquals(1, strategy.getPrimaryRoles().size());
    assertEquals(1, strategy.getUtilityRoles().size());
    assertTrue(strategy.findPrimaryRole("tank").isPresent());
    assertTrue(strategy.findUtilityRole("flex").isPresent());
    assertSame(primary, strategy.findRole("tank").orElseThrow());
    assertSame(utility, strategy.findRole("flex").orElseThrow());
  }

  @Test
  void removeRoleRemovesBothKindsByKey() {
    EventStrategy strategy = new EventStrategy("strat", "Strategy");
    strategy.addRole(new EventRole("tank", "Tank", "shield", 2));
    strategy.addRole(new EventUtilityRole("flex", "Flex", "wrench", 1));

    strategy.removeRole("tank");
    strategy.removeRole("flex");

    assertFalse(strategy.findRole("tank").isPresent());
    assertFalse(strategy.findRole("flex").isPresent());
  }

  @Test
  void guideListIsManagedThroughDomainMethods() {
    EventStrategy strategy = new EventStrategy("strat", "Strategy");

    strategy.addGuide("guide-1");
    strategy.addGuide("guide-2");
    strategy.removeGuide("guide-1");

    assertEquals(1, strategy.getGuides().size());
    assertEquals("guide-2", strategy.getGuides().get(0));
  }
}
