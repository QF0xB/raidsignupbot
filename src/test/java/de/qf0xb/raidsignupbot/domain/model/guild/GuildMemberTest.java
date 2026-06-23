package de.qf0xb.raidsignupbot.domain.model.guild;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuildMemberTest {
  @Test
  void managesActiveGuildsThroughMethodsOnly() {
    GuildMember member = new GuildMember("user-1");
    Guild guild = new Guild("guild-1", "Guild");

    member.addActiveGuild(guild);

    assertEquals(1, member.getActiveGuilds().size());
    assertEquals(guild, member.getActiveGuilds().get(0));
    assertThrows(UnsupportedOperationException.class, () -> member.getActiveGuilds().add(guild));

    member.removeActiveGuild(guild);
    assertEquals(0, member.getActiveGuilds().size());
  }

  @Test
  void priorityResetsWhenAssigned() {
    GuildMember member = new GuildMember("user-1");

    member.increasePriority();
    member.increasePriority();
    member.assignedRole();

    assertEquals(0L, member.getPriority());
  }
}
