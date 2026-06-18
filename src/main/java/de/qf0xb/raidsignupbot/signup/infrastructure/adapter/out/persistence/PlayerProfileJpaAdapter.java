package de.qf0xb.raidsignupbot.signup.infrastructure.adapter.out.persistence;

import de.qf0xb.raidsignupbot.shared.domain.model.PlayerProfile;
import de.qf0xb.raidsignupbot.signup.domain.port.PlayerProfilePort;
import de.qf0xb.raidsignupbot.signup.infrastructure.mapper.PlayerProfilePersistenceMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class PlayerProfileJpaAdapter implements PlayerProfilePort {
  private final PlayerProfileRepository repository;
  private final PlayerProfilePersistenceMapper mapper;

  public PlayerProfileJpaAdapter(PlayerProfileRepository repository, PlayerProfilePersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<PlayerProfile> getByUserId(String userId) {
    return repository.findByUserId(userId)
        .map(mapper::toDomain);
  }

  @Override
  @Transactional
  public void save(PlayerProfile playerProfile) {
    PlayerProfileEntity entity = repository.findByUserId(playerProfile.getUserId())
            .orElseGet(() -> {
              PlayerProfileEntity newEntity = mapper.toEntity(playerProfile);
              repository.persist(newEntity);
              return newEntity;
            });

    mapper.updateEntity(playerProfile, entity);
  }
}
