package de.qf0xb.raidsignupbot.event.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventStrategyTest {
  private EventStrategy eventStrategy;

  @BeforeEach
  void setUp() {
    eventStrategy = new EventStrategy("test", "test");
  }

  @Test
  void testAddRoleDifferentiatingUtilityAndPrimary() {
    EventRole primaryRole = new EventRole("Primary", 1);
    EventRole utilityRole = new EventRole("Utility", 1, true);

    eventStrategy.addRole(primaryRole);
    eventStrategy.addRole(utilityRole);

    assertEquals(1, eventStrategy.getPrimaryRoles().size());
    assertEquals(1, eventStrategy.getUtilities().size());

    assertEquals(primaryRole, eventStrategy.getPrimaryRoles().getFirst());
    assertEquals(utilityRole, eventStrategy.getUtilities().getFirst());
  }

  @Test
  void testAddRoleWithSameNameDifferentiatesPrimaryAndUtility() {
    EventRole primaryRole = new EventRole("Primary", 1);
    EventRole utilityRole = new EventRole("Primary", 1, true);

    eventStrategy.addRole(primaryRole);
    eventStrategy.addRole(utilityRole);

    assertEquals(1, eventStrategy.getPrimaryRoles().size());
    assertEquals(1, eventStrategy.getUtilities().size());
  }
}
