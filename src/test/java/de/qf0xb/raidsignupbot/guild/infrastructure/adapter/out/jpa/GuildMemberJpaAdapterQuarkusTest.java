package de.qf0xb.raidsignupbot.guild.infrastructure.adapter.out.jpa;

import de.qf0xb.raidsignupbot.guild.domain.model.Guild;
import de.qf0xb.raidsignupbot.guild.domain.model.GuildMember;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class GuildMemberJpaAdapterQuarkusTest {
  @Inject
  GuildMemberJpaAdapter guildMemberJpaAdapter;

  @Inject
  EntityManager entityManager;

  @Test
  @TestTransaction
  void findsGuildMemberByUserIdAndGuild() {
    GuildEntity guildEntity = new GuildEntity();
    guildEntity.setGuildId("guild-1");
    guildEntity.setGuildName("Raid Guild");
    guildEntity.setCreateEvents(true);
    guildEntity.setAddRaiderRole(true);
    guildEntity.setRaiderRoleId("raider-role");
    entityManager.persist(guildEntity);

    GuildMemberEntity memberEntity = new GuildMemberEntity();
    memberEntity.setGuild(guildEntity);
    memberEntity.setPlayerId("player-1");
    memberEntity.setPriority(7);
    entityManager.persist(memberEntity);
    entityManager.flush();

    Optional<GuildMember> member = guildMemberJpaAdapter.findByUserIdAndGuild(
        "player-1",
        new Guild("guild-1", "Raid Guild", true, true, "raider-role")
    );

    assertTrue(member.isPresent());
    assertEquals("player-1", member.get().getPlayerId());
    assertEquals(7, member.get().getPriority());
  }
}
