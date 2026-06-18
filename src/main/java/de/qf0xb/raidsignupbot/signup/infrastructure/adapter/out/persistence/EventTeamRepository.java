package de.qf0xb.raidsignupbot.signup.infrastructure.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EventTeamRepository implements PanacheRepositoryBase<EventTeamEntity, Long> {
  public List<EventTeamEntity> findByPlayerUserId(String userId) {
    return find("""
        select distinct team
        from EventTeamEntity team
        join team.assignedRoles assignedRole
        where assignedRole.playerProfile.userId = ?1
        """, userId).list();
  }

  public List<EventTeamEntity> findByEventId(Long eventId) {
    return list("eventId", eventId);
  }

  public List<EventTeamEntity> findByStrategyId(Long strategyId) {
    return list("strategyId", strategyId);
  }

  public Optional<EventTeamEntity> findOptionalById(Long id) {
    return findByIdOptional(id);
  }
}
