package de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.template;

import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class JpaEventTemplateApplicationAdapterTest {
  @Inject
  JpaEventTemplateApplicationAdapter adapter;

  @Inject
  EntityManager entityManager;

  @Test
  @TestTransaction
  void savesAndLoadsTemplateAggregate() {
    EventTemplate initial = new EventTemplate("raid-1", "Raid One", "Initial details", "test");
    EventStrategy strategy = new EventStrategy("storm-test", "Storm Strategy");
    strategy.addGuide("guide-a");
    strategy.addRole(new EventRole("one", "Potent Feint", ":one:", 1));
    EventUtilityRole hex = new EventUtilityRole("hex", "Hex", ":schoolSun-1:", 1);
    hex.addEligiblePrimaryRole("one");
    strategy.addRole(hex);
    initial.addStrategy(strategy);

    EventTemplate saved = adapter.save(initial);
    entityManager.flush();
    entityManager.clear();

    EventTemplate loaded = adapter.findByKey("raid-1").orElseThrow();
    assertEquals("Raid One", loaded.getName());
    assertEquals("Initial details", loaded.getDetails());
    assertEquals(1, loaded.getStrategies().size());
    assertTrue(loaded.findStrategy("storm-test").isPresent());
    assertTrue(loaded.findStrategy("storm-test").orElseThrow().findUtilityRole("hex").orElseThrow().isEligibleFor("one"));

    EventTemplate updated = new EventTemplate("raid-1", "Raid One Updated", "Updated details", "test");
    EventStrategy updatedStrategy = new EventStrategy("storm-test", "Storm Strategy Updated");
    updatedStrategy.addGuide("guide-b");
    updatedStrategy.addRole(new EventRole("one", "Potent Feint Updated", ":one:", 2));
    EventUtilityRole updatedHex = new EventUtilityRole("hex", "Hex Updated", ":schoolSun-1:", 2);
    updatedHex.addEligiblePrimaryRole("one");
    updatedHex.addEligiblePrimaryRole("two");
    updatedStrategy.addRole(updatedHex);
    updated.addStrategy(updatedStrategy);

    adapter.save(updated);
    entityManager.flush();
    entityManager.clear();

    EventTemplate reloaded = adapter.findByKey("raid-1").orElseThrow();
    assertEquals("Raid One Updated", reloaded.getName());
    assertEquals("Updated details", reloaded.getDetails());
    assertEquals(1, reloaded.getStrategies().size());

    EventStrategy reloadedStrategy = reloaded.findStrategy("storm-test").orElseThrow();
    assertEquals("Storm Strategy Updated", reloadedStrategy.getName());
    assertEquals(1, reloadedStrategy.getGuides().size());
    assertTrue(reloadedStrategy.findPrimaryRole("one").isPresent());
    assertEquals(2, reloadedStrategy.findPrimaryRole("one").orElseThrow().getRequiredCount());
    assertEquals(2, reloadedStrategy.findUtilityRole("hex").orElseThrow().getEligiblePrimaryRoleKeys().size());
  }
}
