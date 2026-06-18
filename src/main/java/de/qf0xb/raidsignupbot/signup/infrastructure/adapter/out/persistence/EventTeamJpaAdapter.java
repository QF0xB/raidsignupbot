package de.qf0xb.raidsignupbot.signup.infrastructure.adapter.out.persistence;

import de.qf0xb.raidsignupbot.shared.domain.model.PlayerProfile;
import de.qf0xb.raidsignupbot.signup.domain.model.EventTeam;
import de.qf0xb.raidsignupbot.signup.domain.port.EventTeamPort;
import de.qf0xb.raidsignupbot.signup.infrastructure.mapper.EventTeamPersistenceMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@ApplicationScoped
public class EventTeamJpaAdapter implements EventTeamPort {
  private final EventTeamRepository repository;
  private final EventTeamPersistenceMapper mapper;

  public EventTeamJpaAdapter(EventTeamRepository repository, EventTeamPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<EventTeam> getByPlayer(PlayerProfile playerProfile) {
    return getByPlayerId(playerProfile.getUserId());
  }

  @Override
  public List<EventTeam> getByPlayerId(String playerId) {
    return repository.findByPlayerUserId(playerId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<EventTeam> getByEventId(Long eventId) {
    return repository.findByEventId(eventId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<EventTeam> getByStrategyId(Long strategyId) {
    return repository.findByStrategyId(strategyId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public EventTeam getById(Long id) {
    return repository.findOptionalById(id)
        .map(mapper::toDomain)
        .orElseThrow(() -> new NoSuchElementException("No event team found for id " + id));
  }

  @Override
  @Transactional
  public void save(EventTeam eventTeam) {
    EventTeamEntity entity = eventTeam.getId() == null
        ? mapper.toEntity(eventTeam)
        : repository.findOptionalById(eventTeam.getId()).orElseGet(() -> mapper.toEntity(eventTeam));

    mapper.updateEntity(eventTeam, entity);
    repository.persist(entity);
    eventTeam.setId(entity.getId());
  }
}
