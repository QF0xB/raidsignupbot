package de.qf0xb.raidsignupbot.domain.model.event;

import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Getter
@ToString
public class EventUtilityAssignment {
  private final String key;
  private final String roleName;
  private final String emojiSlug;
  @Getter(AccessLevel.NONE)
  private final Set<String> eligiblePrimaryRoleKeys = new HashSet<>();

  private GuildMember member;

  public EventUtilityAssignment(EventUtilityRole role) {
    key = role.getKey();
    roleName = role.getName();
    emojiSlug = role.getEmojiSlug();
    eligiblePrimaryRoleKeys.addAll(role.getEligiblePrimaryRoleKeys());
  }

  public boolean isEligibleFor(EventSlot slot) {
    return eligiblePrimaryRoleKeys.isEmpty() || eligiblePrimaryRoleKeys.contains(slot.getKey());
  }

  public Set<String> getEligiblePrimaryRoleKeys() {
    return Collections.unmodifiableSet(eligiblePrimaryRoleKeys);
  }

  public boolean matches(String utilityRoleKey) {
    return key.equals(utilityRoleKey);
  }

  public boolean isAssigned() {
    return member != null;
  }

  public void assign(GuildMember member) {
    this.member = member;
  }

  public void unassign() {
    member = null;
  }
}
