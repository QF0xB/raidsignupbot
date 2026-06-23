package de.qf0xb.raidsignupbot.infrastructure.mapper.jpa;

import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventStrategyEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventUtilityRoleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class EventStrategyEntityMapperTest {
  private final EventRoleEntityMapper eventRoleEntityMapper = Mappers.getMapper(EventRoleEntityMapper.class);
  private final EventStrategyEntityMapper eventStrategyEntityMapper = Mappers.getMapper(EventStrategyEntityMapper.class);

  @BeforeEach
  void wireMappers() {
    MapperTestSupport.setField(eventStrategyEntityMapper, "eventRoleEntityMapper", eventRoleEntityMapper);
  }

  @Test
  void mapsStrategyAggregateBothDirections() {
    EventStrategy strategy = new EventStrategy("myth", "Myth Strategy");
    strategy.addGuide("start-here");
    strategy.addRole(new EventRole("a", "Potent Feint", ":a:", 1));
    EventUtilityRole hex = new EventUtilityRole("hex", "Hex", ":schoolSun-1:", 2);
    hex.addEligiblePrimaryRole("a");
    hex.addEligiblePrimaryRole("b");
    strategy.addRole(hex);

    EventStrategyEntity entity = eventStrategyEntityMapper.toEntity(strategy);

    assertEquals("myth", entity.getKey());
    assertEquals("Myth Strategy", entity.getName());
    assertEquals(1, entity.getGuides().size());
    assertEquals(2, entity.getEventRoleEntities().size());
    assertTrue(entity.getEventRoleEntities().stream().anyMatch(role -> role instanceof EventUtilityRoleEntity));

    EventStrategy roundTrip = eventStrategyEntityMapper.toDomain(entity);

    assertEquals("myth", roundTrip.getKey());
    assertEquals("Myth Strategy", roundTrip.getName());
    assertEquals(1, roundTrip.getGuides().size());
    assertTrue(roundTrip.findPrimaryRole("a").isPresent());
    assertTrue(roundTrip.findUtilityRole("hex").isPresent());
    assertEquals(2, roundTrip.findUtilityRole("hex").orElseThrow().getEligiblePrimaryRoleKeys().size());
  }
}
