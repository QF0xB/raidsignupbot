package de.qf0xb.raidsignupbot.signup.infrastructure.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class PlayerProfileRepository implements PanacheRepositoryBase<PlayerProfileEntity, Long> {
  public Optional<PlayerProfileEntity> findByUserId(String userId) {
    return find("userId", userId).firstResultOptional();
  }
}
