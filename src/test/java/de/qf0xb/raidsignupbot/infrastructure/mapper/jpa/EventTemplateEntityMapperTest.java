package de.qf0xb.raidsignupbot.infrastructure.mapper.jpa;

import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventStrategyEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventTemplateEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventUtilityRoleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class EventTemplateEntityMapperTest {
  private final EventRoleEntityMapper eventRoleEntityMapper = Mappers.getMapper(EventRoleEntityMapper.class);
  private final EventStrategyEntityMapper eventStrategyEntityMapper = Mappers.getMapper(EventStrategyEntityMapper.class);
  private final EventTemplateEntityMapper eventTemplateEntityMapper = Mappers.getMapper(EventTemplateEntityMapper.class);

  @BeforeEach
  void wireMappers() {
    MapperTestSupport.setField(eventStrategyEntityMapper, "eventRoleEntityMapper", eventRoleEntityMapper);
    MapperTestSupport.setField(eventTemplateEntityMapper, "eventStrategyEntityMapper", eventStrategyEntityMapper);
  }

  @Test
  void mapsTemplateAggregateBothDirections() {
    EventTemplate template = new EventTemplate("large-test", "Large Test Raid", "Stress test", "test");

    EventStrategy storm = new EventStrategy("storm", "Storm Strategy");
    storm.addGuide("guide-1");
    storm.addRole(new EventRole("one", "Potent Feint", ":one:", 1));
    EventUtilityRole hex = new EventUtilityRole("hex", "Hex", ":schoolSun-1:", 1);
    hex.addEligiblePrimaryRole("one");
    storm.addRole(hex);
    template.addStrategy(storm);

    EventTemplateEntity entity = eventTemplateEntityMapper.toEntity(template);

    assertEquals("large-test", entity.getKey());
    assertEquals("Large Test Raid", entity.getName());
    assertEquals("Stress test", entity.getDetails());
    assertEquals("test", entity.getEmojiSlug());
    assertEquals(1, entity.getEventStrategies().size());

    EventStrategyEntity strategyEntity = entity.getEventStrategies().iterator().next();
    assertSame(entity, strategyEntity.getEventTemplateEntity());
    assertEquals("storm", strategyEntity.getKey());
    assertEquals("Storm Strategy", strategyEntity.getName());
    assertEquals(1, strategyEntity.getGuides().size());
    assertEquals(2, strategyEntity.getEventRoleEntities().size());
    assertTrue(strategyEntity.getEventRoleEntities().stream().anyMatch(role -> role instanceof EventUtilityRoleEntity));

    EventTemplate roundTrip = eventTemplateEntityMapper.toDomain(entity);

    assertEquals("large-test", roundTrip.getKey());
    assertEquals("Large Test Raid", roundTrip.getName());
    assertEquals("Stress test", roundTrip.getDetails());
    assertEquals("test", roundTrip.getEmojiSlug());
    assertEquals(1, roundTrip.getStrategies().size());

    EventStrategy roundTripStrategy = roundTrip.findStrategy("storm").orElseThrow();
    assertEquals("Storm Strategy", roundTripStrategy.getName());
    assertEquals(1, roundTripStrategy.getGuides().size());
    assertTrue(roundTripStrategy.findPrimaryRole("one").isPresent());
    assertTrue(roundTripStrategy.findUtilityRole("hex").isPresent());
    assertEquals(1, roundTripStrategy.findUtilityRole("hex").orElseThrow().getEligiblePrimaryRoleKeys().size());
    assertTrue(roundTripStrategy.findUtilityRole("hex").orElseThrow().isEligibleFor("one"));
  }
}
