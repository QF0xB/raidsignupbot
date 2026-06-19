package de.qf0xb.raidsignupbot.domain.model.event;

import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventMemberSignupTest {
  @Test
  void storesRoleKeysAndDeduplicatesThem() {
    EventMemberSignup signup = new EventMemberSignup(new GuildMember("user-1"));

    signup.addRoleKey("tank");
    signup.addRole(new EventRole("tank", "Tank", "tank", 1));
    signup.addRoleKey("healer");

    assertEquals(2, signup.getRoleKeys().size());
    assertTrue(signup.wantsRole("tank"));
    assertTrue(signup.wantsRole("healer"));
    assertFalse(signup.wantsRole("flex"));
  }
}
