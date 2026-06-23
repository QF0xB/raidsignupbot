package de.qf0xb.raidsignupbot.infrastructure.mapper.jpa;

import org.mapstruct.MappingTarget;

public interface JpaEntityMapper<E, D> {
  E toEntity(D dto);
  D toDomain(E entity);
  E partialUpdate(D dto, @MappingTarget E entity);
}
