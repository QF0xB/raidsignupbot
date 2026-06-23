package de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa;

import de.qf0xb.raidsignupbot.infrastructure.mapper.jpa.JpaEntityMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.transaction.Transactional;

public abstract class AbstractJpaApplicationAdapter<ID, D, E, M extends JpaEntityMapper<E, D>, R extends PanacheRepositoryBase<E, ID>> {
  protected abstract M getMapper();

  protected abstract R getRepository();

  @Transactional
  public D save(ID id, D domain) {
    E entity = id == null
        ? null
        : getRepository().findByIdOptional(id).orElse(null);

    if (entity == null) {
      E newEntity = getMapper().toEntity(domain);
      getRepository().persist(newEntity);
      return getMapper().toDomain(newEntity);
    }

    entity = getMapper().partialUpdate(domain, entity);
    return getMapper().toDomain(entity);
  }

  public abstract E partialUpdate(D domain, E entity);
}
