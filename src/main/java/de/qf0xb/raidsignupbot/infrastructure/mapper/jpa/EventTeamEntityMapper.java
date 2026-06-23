package de.qf0xb.raidsignupbot.infrastructure.mapper.jpa;

import de.qf0xb.raidsignupbot.domain.model.event.EventSlot;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.event.EventUtilityAssignment;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.shared.RoleKind;
import de.qf0xb.raidsignupbot.infrastructure.jpa.event.EventTeamAssignmentEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.event.EventTeamEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ObjectFactory;
import org.mapstruct.ReportingPolicy;

import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
    uses = {EventStrategyEntityMapper.class, GuildMemberEntityMapper.class})
public abstract class EventTeamEntityMapper implements JpaEntityMapper<EventTeamEntity, EventTeam> {
  @Inject
  EventStrategyEntityMapper eventStrategyEntityMapper;

  @Inject
  GuildMemberEntityMapper guildMemberEntityMapper;

  @Override
  @Mapping(target = "event", ignore = true)
  @Mapping(target = "eventStrategy", source = "strategy")
  @Mapping(target = "assignments", ignore = true)
  public abstract EventTeamEntity toEntity(EventTeam eventTeam);

  @Override
  @Mapping(target = "strategy", ignore = true)
  @Mapping(target = "slots", ignore = true)
  @Mapping(target = "utilityAssignments", ignore = true)
  public abstract EventTeam toDomain(EventTeamEntity eventTeamEntity);

  @Override
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "event", ignore = true)
  @Mapping(target = "eventStrategy", source = "strategy")
  @Mapping(target = "assignments", ignore = true)
  public abstract EventTeamEntity partialUpdate(
      EventTeam eventTeam, @MappingTarget EventTeamEntity eventTeamEntity);

  @ObjectFactory
  protected EventTeam createDomain(EventTeamEntity eventTeamEntity) {
    return new EventTeam(eventStrategyEntityMapper.toDomain(eventTeamEntity.getEventStrategy()));
  }

  @AfterMapping
  protected void populateDomain(EventTeamEntity source, @MappingTarget EventTeam target) {
    source.getAssignments().stream()
        .filter(assignment -> assignment.getRoleKind() == RoleKind.PRIMARY)
        .forEach(
            assignment -> target.assign(assignment.getRoleKey(), guildMemberEntityMapper.toDomain(assignment.getMember())));
    source.getAssignments().stream()
        .filter(assignment -> assignment.getRoleKind() == RoleKind.UTILITY)
        .forEach(
            assignment -> target.assignUtility(
                assignment.getRoleKey(), guildMemberEntityMapper.toDomain(assignment.getMember())));
  }

  @AfterMapping
  protected void populateEntity(EventTeam source, @MappingTarget EventTeamEntity target) {
    syncAssignments(target, source);
  }

  private void syncAssignments(EventTeamEntity target, EventTeam source) {
    Map<String, EventTeamAssignmentEntity> existingByKey = new LinkedHashMap<>();
    for (EventTeamAssignmentEntity assignment : target.getAssignments()) {
      existingByKey.put(key(assignment.getRoleKind(), assignment.getRoleKey()), assignment);
    }

    Set<EventTeamAssignmentEntity> updatedAssignments = new LinkedHashSet<>();
    for (EventSlot slot : source.getSlots()) {
      if (slot.isAssigned()) {
        updatedAssignments.add(syncAssignment(
            target,
            existingByKey,
            RoleKind.PRIMARY,
            slot.getKey(),
            slot.getMember()));
      }
    }
    for (EventUtilityAssignment assignment : source.getUtilityAssignments()) {
      if (assignment.isAssigned()) {
        updatedAssignments.add(syncAssignment(
            target,
            existingByKey,
            RoleKind.UTILITY,
            assignment.getKey(),
            assignment.getMember()));
      }
    }

    target.getAssignments().clear();
    target.getAssignments().addAll(updatedAssignments);
  }

  private EventTeamAssignmentEntity syncAssignment(
      EventTeamEntity target,
      Map<String, EventTeamAssignmentEntity> existingByKey,
      RoleKind roleKind,
      String roleKey,
      GuildMember member) {
    String key = key(roleKind, roleKey);
    EventTeamAssignmentEntity entity = existingByKey.remove(key);
    if (entity == null) {
      entity = new EventTeamAssignmentEntity();
    }

    entity.setEventTeam(target);
    entity.setRoleKind(roleKind);
    entity.setRoleKey(roleKey);
    entity.setMember(guildMemberEntityMapper.toEntity(member));
    return entity;
  }

  private String key(RoleKind roleKind, String roleKey) {
    return roleKind + ":" + roleKey;
  }
}
