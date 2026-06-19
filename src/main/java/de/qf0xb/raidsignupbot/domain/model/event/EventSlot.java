package de.qf0xb.raidsignupbot.domain.model.event;

import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.shared.RoleKind;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class EventSlot {
  private final String key;
  private final String roleName;
  private final String emojiSlug;
  private final RoleKind roleKind = RoleKind.PRIMARY;

  private GuildMember member;

  public EventSlot(EventRole role) {
    if (role instanceof EventUtilityRole) {
      throw new IllegalArgumentException("Utility roles are not primary team slots");
    }
    key = role.getKey();
    roleName = role.getName();
    emojiSlug = role.getEmojiSlug();
  }

  public boolean isPrimary() {
    return roleKind == RoleKind.PRIMARY;
  }

  public boolean isUtility() {
    return roleKind == RoleKind.UTILITY;
  }

  public boolean matches(String roleKey) {
    return roleKey.equals(key);
  }

  public void assign(GuildMember member) {
    this.member = member;
  }

  public boolean isAssigned() {
    return member != null;
  }

  public void unassign() {
    this.member = null;
  }
}
