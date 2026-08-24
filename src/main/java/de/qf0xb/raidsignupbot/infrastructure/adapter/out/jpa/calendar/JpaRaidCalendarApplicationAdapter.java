package de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.calendar;

import de.qf0xb.raidsignupbot.application.port.RaidCalendarApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.calendar.RaidCalendarEntry;
import de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.AbstractJpaApplicationAdapter;
import de.qf0xb.raidsignupbot.infrastructure.jpa.calendar.RaidCalendarEntryEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.calendar.RaidCalendarEntryEntityRepository;
import de.qf0xb.raidsignupbot.infrastructure.mapper.jpa.RaidCalendarEntryEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JpaRaidCalendarApplicationAdapter
    extends AbstractJpaApplicationAdapter<Long, RaidCalendarEntry, RaidCalendarEntryEntity,
        RaidCalendarEntryEntityMapper, RaidCalendarEntryEntityRepository>
    implements RaidCalendarApplicationPort {
  @Inject
  RaidCalendarEntryEntityMapper mapper;

  @Inject
  RaidCalendarEntryEntityRepository repository;

  @Override
  protected RaidCalendarEntryEntityMapper getMapper() {
    return mapper;
  }

  @Override
  protected RaidCalendarEntryEntityRepository getRepository() {
    return repository;
  }

  @Override
  @Transactional
  public Optional<RaidCalendarEntry> findById(Long id) {
    return repository.findByIdOptional(id).map(mapper::toDomain);
  }

  @Override
  @Transactional
  public List<RaidCalendarEntry> findByServerId(String serverId) {
    return repository.list("serverId = ?1 order by startAt", serverId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  @Transactional
  public RaidCalendarEntry save(RaidCalendarEntry entry) {
    return save(entry.getId(), entry);
  }

  @Override
  @Transactional
  public void deleteById(Long id) {
    repository.deleteById(id);
  }

  @Override
  public RaidCalendarEntryEntity partialUpdate(RaidCalendarEntry domain, RaidCalendarEntryEntity entity) {
    return mapper.partialUpdate(domain, entity);
  }
}
