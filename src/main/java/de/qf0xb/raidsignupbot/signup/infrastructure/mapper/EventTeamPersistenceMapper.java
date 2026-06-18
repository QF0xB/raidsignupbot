package de.qf0xb.raidsignupbot.signup.infrastructure.mapper;

import de.qf0xb.raidsignupbot.shared.domain.model.EventRole;
import de.qf0xb.raidsignupbot.signup.domain.model.EventAssignedRole;
import de.qf0xb.raidsignupbot.signup.domain.model.EventPlayerSignup;
import de.qf0xb.raidsignupbot.signup.domain.model.EventTeam;
import de.qf0xb.raidsignupbot.signup.infrastructure.adapter.out.persistence.EventAssignedRoleEntity;
import de.qf0xb.raidsignupbot.signup.infrastructure.adapter.out.persistence.EventRoleEntity;
import de.qf0xb.raidsignupbot.signup.infrastructure.adapter.out.persistence.EventRoleRepository;
import de.qf0xb.raidsignupbot.signup.infrastructure.adapter.out.persistence.EventTeamEntity;
import de.qf0xb.raidsignupbot.signup.infrastructure.adapter.out.persistence.PlayerProfileEntity;
import de.qf0xb.raidsignupbot.signup.infrastructure.adapter.out.persistence.PlayerProfileRepository;
import de.qf0xb.raidsignupbot.template.domain.model.EventStrategyTemplate;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class EventTeamPersistenceMapper {
  private final PlayerProfileRepository playerProfileRepository;
  private final EventRoleRepository eventRoleRepository;

  public EventTeamPersistenceMapper(PlayerProfileRepository playerProfileRepository, EventRoleRepository eventRoleRepository) {
    this.playerProfileRepository = playerProfileRepository;
    this.eventRoleRepository = eventRoleRepository;
  }

  public EventTeam toDomain(EventTeamEntity entity) {
    EventStrategyTemplate strategy = new EventStrategyTemplate(
        entity.getStrategyId(),
        entity.getStrategyName(),
        List.of(),
        List.of()
    );
    EventTeam team = new EventTeam(entity.getEventId(), strategy);
    team.setId(entity.getId());
    entity.getAssignedRoles().stream()
        .map(this::toDomain)
        .forEach(team::addAssignedRole);
    return team;
  }

  public EventTeamEntity toEntity(EventTeam domain) {
    EventTeamEntity entity = new EventTeamEntity();
    updateEntity(domain, entity);
    return entity;
  }

  public void updateEntity(EventTeam domain, EventTeamEntity entity) {
    entity.setEventId(domain.getEventId());
    entity.setStrategyId(domain.getStrategy() == null ? null : domain.getStrategy().getId());
    entity.setStrategyName(domain.getStrategy() == null ? "Unknown strategy" : domain.getStrategy().getName());
    entity.getAssignedRoles().clear();

    for (EventAssignedRole assignedRole : domain.getAssignedRoles()) {
      entity.addAssignedRole(toEntity(assignedRole));
    }
  }

  private EventAssignedRole toDomain(EventAssignedRoleEntity entity) {
    EventPlayerSignup player = new EventPlayerSignup(entity.getPlayerProfile().getUserId());
    return new EventAssignedRole(
        player,
        toDomain(entity.getRole()),
        entity.getUtilityRole() == null ? null : toDomain(entity.getUtilityRole())
    );
  }

  private EventAssignedRoleEntity toEntity(EventAssignedRole domain) {
    EventAssignedRoleEntity entity = new EventAssignedRoleEntity();
    PlayerProfileEntity playerProfile = playerProfileRepository.getOrCreate(domain.getPlayer().getPlayerIdentity());
    entity.setPlayerProfile(playerProfile);
    entity.setRole(eventRoleRepository.getOrCreate(domain.getRole()));
    if (domain.getUtilityRole() != null) {
      entity.setUtilityRole(eventRoleRepository.getOrCreate(domain.getUtilityRole()));
    }
    return entity;
  }

  private EventRole toDomain(EventRoleEntity entity) {
    EventRole role = new EventRole(entity.getName(), entity.getCount());
    role.setId(entity.getRoleTemplateId());
    return role;
  }
}
