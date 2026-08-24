package de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.guild;

import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.infrastructure.jpa.server.ServerEntity;
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
    ServerEntity server = new ServerEntity();
    server.setServerId("guild-1");
    entityManager.persist(server);

    Guild initial = new Guild("Initial Guild", "guild-1");
    initial.setRaiderRolesEnabled(false);
    initial.setRaiderRoleId("raider-1");

    adapter.save(initial);
    entityManager.flush();
    entityManager.clear();

    Guild loaded = adapter.findByName("Initial Guild").orElseThrow();
    assertEquals("Initial Guild", loaded.getName());
    assertEquals("raider-1", loaded.getRaiderRoleId());
    assertFalse(loaded.isRaiderRolesEnabled());

    Guild updated = new Guild("Updated Guild", "guild-1");
    updated.setRaiderRolesEnabled(true);
    updated.setRaiderRoleId("raider-2");

    adapter.save(updated);
    entityManager.flush();
    entityManager.clear();

    Guild reloaded = adapter.findByName("Updated Guild").orElseThrow();
    assertEquals("Updated Guild", reloaded.getName());
    assertEquals("raider-2", reloaded.getRaiderRoleId());
    assertTrue(reloaded.isRaiderRolesEnabled());
  }
}
