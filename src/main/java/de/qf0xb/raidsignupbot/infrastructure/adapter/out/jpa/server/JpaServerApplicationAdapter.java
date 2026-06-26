package de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.server;

import de.qf0xb.raidsignupbot.application.port.server.ServerApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.AbstractJpaApplicationAdapter;
import de.qf0xb.raidsignupbot.infrastructure.jpa.server.ServerEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.server.ServerEntityRepository;
import de.qf0xb.raidsignupbot.infrastructure.mapper.jpa.ServerEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class JpaServerApplicationAdapter
    extends AbstractJpaApplicationAdapter<String, Server, ServerEntity, ServerEntityMapper, ServerEntityRepository>
    implements ServerApplicationPort {
  @Inject
  ServerEntityMapper mapper;

  @Inject
  ServerEntityRepository repository;

  @Override
  protected ServerEntityMapper getMapper() {
    return mapper;
  }

  @Override
  protected ServerEntityRepository getRepository() {
    return repository;
  }

  @Override
  @Transactional
  public Optional<Server> findByServerId(String serverId) {
    return getRepository().findByIdOptional(serverId).map(getMapper()::toDomain);
  }

  @Override
  @Transactional
  public Server save(Server server) {
    return save(server.getServerId(), server);
  }

  @Override
  public ServerEntity partialUpdate(Server domain, ServerEntity entity) {
    return getMapper().partialUpdate(domain, entity);
  }
}
