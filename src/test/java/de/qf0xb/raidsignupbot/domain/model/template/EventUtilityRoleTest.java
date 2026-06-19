package de.qf0xb.raidsignupbot.domain.model.template;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventUtilityRoleTest {
  @Test
  void emptyEligibilityAllowsAllPrimaryRoles() {
    EventUtilityRole utilityRole = new EventUtilityRole("flex", "Flex", "wrench", 1);

    assertTrue(utilityRole.isEligibleFor("tank"));
    assertTrue(utilityRole.isEligibleFor(new EventRole("healer", "Healer", "plus", 1)));
  }

  @Test
  void explicitEligibilityRestrictsToConfiguredRoles() {
    EventRole tank = new EventRole("tank", "Tank", "shield", 1);
    EventRole healer = new EventRole("healer", "Healer", "plus", 1);
    EventUtilityRole utilityRole = new EventUtilityRole("flex", "Flex", "wrench", 1);

    utilityRole.addEligiblePrimaryRole(tank);

    assertTrue(utilityRole.isEligibleFor("tank"));
    assertTrue(utilityRole.isEligibleFor(tank));
    assertFalse(utilityRole.isEligibleFor("healer"));

    utilityRole.addEligiblePrimaryRole(healer);
    assertTrue(utilityRole.isEligibleFor("healer"));

    utilityRole.removeEligiblePrimaryRole("tank");
    assertFalse(utilityRole.isEligibleFor("tank"));
  }
}
