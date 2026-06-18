package de.qf0xb.raidsignupbot.guild.infrastructure.adapter.out.jpa;

import de.qf0xb.raidsignupbot.guild.domain.model.Guild;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class GuildJpaAdapterQuarkusTest {
  @Inject
  GuildJpaAdapter guildJpaAdapter;

  @Test
  @TestTransaction
  void savesAndLoadsGuildThroughDatabase() {
    Guild guild = new Guild("guild-1", "Raid Guild", true, false, "raider-role");

    Guild saved = guildJpaAdapter.save(guild);
    Optional<Guild> loaded = guildJpaAdapter.findByGuildId("guild-1");

    assertTrue(loaded.isPresent());
    assertEquals(saved.getGuildId(), loaded.get().getGuildId());
    assertEquals(saved.getGuildName(), loaded.get().getGuildName());
    assertEquals(saved.isCreateEvents(), loaded.get().isCreateEvents());
    assertEquals(saved.isAddRaiderRole(), loaded.get().isAddRaiderRole());
    assertEquals(saved.getRaiderRoleId(), loaded.get().getRaiderRoleId());
  }

  @Test
  @TestTransaction
  void updatesExistingGuild() {
    Guild guild = new Guild("guild-1", "Raid Guild", true, false, "raider-role");
    guildJpaAdapter.save(guild);

    guild.setGuildName("Updated Guild");
    guildJpaAdapter.save(guild);

    Optional<Guild> loaded = guildJpaAdapter.findByGuildId("guild-1");
    assertTrue(loaded.isPresent());
    assertEquals("Updated Guild", loaded.get().getGuildName());
  }
}
