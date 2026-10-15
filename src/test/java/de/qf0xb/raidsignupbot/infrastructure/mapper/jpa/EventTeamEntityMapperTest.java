package de.qf0xb.raidsignupbot.infrastructure.mapper.jpa;

import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.event.EventUtilityAssignment;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.shared.RoleKind;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import de.qf0xb.raidsignupbot.infrastructure.jpa.event.EventTeamEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class EventTeamEntityMapperTest {
  private final EventRoleEntityMapper eventRoleEntityMapper = Mappers.getMapper(EventRoleEntityMapper.class);
  private final EventStrategyEntityMapper eventStrategyEntityMapper = Mappers.getMapper(EventStrategyEntityMapper.class);
  private final GuildMemberEntityMapper guildMemberEntityMapper = Mappers.getMapper(GuildMemberEntityMapper.class);
  private final EventTeamEntityMapper eventTeamEntityMapper = Mappers.getMapper(EventTeamEntityMapper.class);

  @BeforeEach
  void wireMappers() {
    MapperTestSupport.setField(eventStrategyEntityMapper, "eventRoleEntityMapper", eventRoleEntityMapper);
    MapperTestSupport.setField(guildMemberEntityMapper, "guildEntityMapper", Mappers.getMapper(GuildEntityMapper.class));
    MapperTestSupport.setField(eventTeamEntityMapper, "eventStrategyEntityMapper", eventStrategyEntityMapper);
    MapperTestSupport.setField(eventTeamEntityMapper, "guildMemberEntityMapper", guildMemberEntityMapper);
  }

  @Test
  void mapsTeamAssignmentsBothDirections() {
    EventStrategy strategy = new EventStrategy("storm", "Storm Strategy");
    strategy.addRole(new EventRole("tank", "Tank", ":shield:", 1));
    EventUtilityRole battleRes = new EventUtilityRole("battle-res", "Battle Res", ":crossed_swords:", 1);
    battleRes.addEligiblePrimaryRole("tank");
    strategy.addRole(battleRes);

    EventTeam team = new EventTeam(strategy);
    GuildMember member = new GuildMember("user-1");
    team.assign("tank", member);
    team.assignUtility("battle-res", member);

    EventTeamEntity entity = eventTeamEntityMapper.toEntity(team);

    assertEquals("storm", entity.getEventStrategy().getKey());
    assertEquals(2, entity.getAssignments().size());
    assertTrue(entity.getAssignments().stream().anyMatch(assignment -> assignment.getRoleKind() == RoleKind.PRIMARY));
    assertTrue(entity.getAssignments().stream().anyMatch(assignment -> assignment.getRoleKind() == RoleKind.UTILITY));

    EventTeam roundTrip = eventTeamEntityMapper.toDomain(entity);

    assertEquals("storm", roundTrip.getStrategy().getKey());
    assertEquals(1, roundTrip.getSlots().stream().filter(slot -> slot.isAssigned()).count());
    assertEquals(1, roundTrip.getUtilityAssignments().stream().filter(EventUtilityAssignment::isAssigned).count());
    assertEquals("user-1", roundTrip.getSlots().stream().filter(slot -> slot.isAssigned()).findFirst().orElseThrow().getMember().getUserId());
    assertEquals("user-1", roundTrip.getUtilityAssignments().stream().filter(assignment -> assignment.isAssigned()).findFirst().orElseThrow().getMember().getUserId());
  }

  @Test
  void preservesTwoAssignmentsForTheSamePlayerOnSaveAndReload() {
    EventStrategy strategy = new EventStrategy("main", "Main");
    strategy.addRole(new EventRole("fire", "Fire", "🔥", 2));
    EventUtilityRole hex = new EventUtilityRole("hex", "Hex", "☀️", 1);
    hex.addEligiblePrimaryRole("fire");
    strategy.addRole(hex);
    EventTeam team = new EventTeam(strategy);
    GuildMember member = new GuildMember("dual");
    team.assign("fire", member);
    team.assign("fire", member);
    team.assignUtility("hex", member);

    EventTeamEntity entity = eventTeamEntityMapper.toEntity(team);
    eventTeamEntityMapper.partialUpdate(team, entity);
    EventTeam reloaded = eventTeamEntityMapper.toDomain(entity);

    assertEquals(3, entity.getAssignments().size());
    assertEquals(2, reloaded.getSlots().stream()
        .filter(slot -> slot.isAssigned() && slot.getMember().getUserId().equals("dual")).count());
    assertEquals("dual", reloaded.getUtilityAssignments().getFirst().getMember().getUserId());
    assertTrue(reloaded.isFilled());
  }
}
