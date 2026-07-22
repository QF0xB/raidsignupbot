package de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.guild;

import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class JpaGuildApplicationAdapterTest {
  @Inject
  JpaGuildApplicationAdapter adapter;

  @Inject
  EntityManager entityManager;

  @Test
  @TestTransaction
  void savesAndLoadsGuild() {
    Guild initial = new Guild("guild-1", "guild-1");
    initial.setRaiderRolesEnabled(false);
    initial.setRaiderRoleId("raider-1");

    adapter.save(initial);
    entityManager.flush();
    entityManager.clear();

    Guild loaded = adapter.findByName("guild-1").orElseThrow();
    assertEquals("Initial Guild", loaded.getName());
    assertEquals("raider-1", loaded.getRaiderRoleId());
    assertFalse(loaded.isRaiderRolesEnabled());

    Guild updated = new Guild("haeihaahei", "guild-1");
    updated.setRaiderRolesEnabled(true);
    updated.setRaiderRoleId("raider-2");

    adapter.save(updated);
    entityManager.flush();
    entityManager.clear();

    Guild reloaded = adapter.findByName("guild-1").orElseThrow();
    assertEquals("Updated Guild", reloaded.getName());
    assertEquals("raider-2", reloaded.getRaiderRoleId());
    assertTrue(reloaded.isRaiderRolesEnabled());
  }
}
