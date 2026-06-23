package de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa;

import de.qf0xb.raidsignupbot.infrastructure.mapper.jpa.JpaEntityMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.Getter;

@Getter
abstract class AbstractJpaApplicationAdapter<ID, D, E, M extends JpaEntityMapper<E, D>, R extends PanacheRepositoryBase<E, ID>> {
  @Inject
  M mapper;

  @Inject
  R repository;

  @Transactional
  public D save(ID id, D domain) {
    E entity = repository.findByIdOptional(id)
        .orElseGet(() -> {
          E newEntity = mapper.toEntity(domain);
          repository.persist(newEntity);
          return newEntity;
        });

    entity = mapper.partialUpdate(domain, entity);
    return mapper.toDomain(entity);
  }
}
