package de.qf0xb.raidsignupbot.domain.model.template;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventTemplateTest {
  @Test
  void canFindAndRequireStrategiesByKey() {
    EventTemplate template = new EventTemplate("temp", "temp", "temp", "test");
    EventStrategy strategy = new EventStrategy("strat", "Strategy");
    template.addStrategy(strategy);

    assertEquals(strategy, template.findStrategy("strat").orElseThrow());
    assertEquals(strategy, template.requireStrategy("strat"));
  }

  @Test
  void requireStrategyFailsForUnknownKey() {
    EventTemplate template = new EventTemplate("temp", "temp", "temp", "test");

    assertThrows(IllegalStateException.class, () -> template.requireStrategy("missing"));
  }
}
