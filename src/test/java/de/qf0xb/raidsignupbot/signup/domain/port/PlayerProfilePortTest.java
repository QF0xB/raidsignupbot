package de.qf0xb.raidsignupbot.signup.domain.port;

import de.qf0xb.raidsignupbot.shared.domain.model.PlayerProfile;
import de.qf0xb.raidsignupbot.signup.infrastructure.adapter.out.persistence.PlayerProfileRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PlayerProfilePortTest {
  @Inject
  PlayerProfilePort playerProfilePort;

  @Inject
  PlayerProfileRepository playerProfileRepository;

  @Test
  @TestTransaction
  void savesAndLoadsPlayerProfile() {
    PlayerProfile playerProfile = new PlayerProfile("123456789");
    playerProfile.recordUnmatchedSignup();
    playerProfilePort.save(playerProfile);

    Optional<PlayerProfile> result = playerProfilePort.getByUserId("123456789");
    assertTrue(result.isPresent());
    assertEquals(1, result.get().getPriorityScore());
  }

  @Test
  @TestTransaction
  void savingExistingPlayerProfileUpdatesExistingRow() {
    PlayerProfile firstProfileState = new PlayerProfile("existing-user");
    firstProfileState.recordUnmatchedSignup();
    playerProfilePort.save(firstProfileState);

    PlayerProfile secondProfileState = new PlayerProfile("existing-user");
    secondProfileState.setPriorityScore(5);
    playerProfilePort.save(secondProfileState);

    Optional<PlayerProfile> result = playerProfilePort.getByUserId("existing-user");
    assertTrue(result.isPresent());
    assertEquals(5, result.get().getPriorityScore());
    assertEquals(1, playerProfileRepository.count("userId", "existing-user"));
  }
}
