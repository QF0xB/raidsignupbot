package de.qf0xb.raidsignupbot.signup.domain.port;

import de.qf0xb.raidsignupbot.shared.domain.model.PlayerProfile;

import java.util.Optional;

public interface PlayerProfilePort {
  Optional<PlayerProfile> getByUserId(String userId);
  void save(PlayerProfile playerProfile);
}
