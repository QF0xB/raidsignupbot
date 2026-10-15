package de.qf0xb.raidsignupbot.domain.model.event;

import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventMemberSignupTest {
  @Test
  void dualBoxIsOptInAndIndependentOfBackupPriority() {
    GuildMember member = new GuildMember("user-1");
    member.setPriority(10);
    EventMemberSignup signup = new EventMemberSignup(member, "fire");
    assertEquals(1, signup.getMaxAssignments());
    assertFalse(signup.wantsDualBox());

    signup.addRoleKey(EventMemberSignup.DUAL_BOX_ROLE_KEY);
    assertTrue(signup.wantsDualBox());
    assertEquals(2, signup.getMaxAssignments());
    assertEquals(10, signup.getEffectivePriority());
    signup.addRoleKey(EventMemberSignup.BACKUP_ROLE_KEY);
    assertEquals(2, signup.getMaxAssignments());
    assertEquals(10 - EventMemberSignup.BACKUP_PRIORITY_PENALTY, signup.getEffectivePriority());

    signup.removeRoleKey(EventMemberSignup.DUAL_BOX_ROLE_KEY);
    assertEquals(1, signup.getMaxAssignments());
  }

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
