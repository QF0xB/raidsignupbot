package de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.guild;

import de.qf0xb.raidsignupbot.application.port.guild.RaiderRoleGrantApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.guild.RaiderRoleGrant;
import de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.AbstractJpaApplicationAdapter;
import de.qf0xb.raidsignupbot.infrastructure.jpa.guild.RaiderRoleGrantEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.guild.RaiderRoleGrantEntityRepository;
import de.qf0xb.raidsignupbot.infrastructure.mapper.jpa.RaiderRoleGrantEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class JpaRaiderRoleGrantApplicationAdapter
    extends AbstractJpaApplicationAdapter<Long, RaiderRoleGrant, RaiderRoleGrantEntity,
        RaiderRoleGrantEntityMapper, RaiderRoleGrantEntityRepository>
    implements RaiderRoleGrantApplicationPort {
  @Inject
  RaiderRoleGrantEntityMapper mapper;

  @Inject
  RaiderRoleGrantEntityRepository repository;

  @Override
  protected RaiderRoleGrantEntityMapper getMapper() {
    return mapper;
  }

  @Override
  protected RaiderRoleGrantEntityRepository getRepository() {
    return repository;
  }

  @Override
  @Transactional
  public List<RaiderRoleGrant> findByEventId(Long eventId) {
    return repository.list("eventId", eventId).stream().map(mapper::toDomain).toList();
  }

  @Override
  @Transactional
  public List<RaiderRoleGrant> findReleasableBefore(Instant now) {
    return repository.list("releaseAfter < ?1", now).stream().map(mapper::toDomain).toList();
  }

  @Override
  @Transactional
  public long countGrants(String serverId, String userId, String roleId) {
    return repository.count("serverId = ?1 and userId = ?2 and roleId = ?3", serverId, userId, roleId);
  }

  @Override
  @Transactional
  public RaiderRoleGrant save(RaiderRoleGrant grant) {
    return save(grant.getId(), grant);
  }

  @Override
  @Transactional
  public void deleteById(Long id) {
    repository.deleteById(id);
  }

  @Override
  public RaiderRoleGrantEntity partialUpdate(RaiderRoleGrant domain, RaiderRoleGrantEntity entity) {
    return mapper.partialUpdate(domain, entity);
  }
}
