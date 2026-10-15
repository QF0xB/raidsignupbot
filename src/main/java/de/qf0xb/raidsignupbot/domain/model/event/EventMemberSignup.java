package de.qf0xb.raidsignupbot.domain.model.event;

import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Collections;
import java.util.Set;

@Getter
@RequiredArgsConstructor
@ToString
public class EventMemberSignup {
  public static final String BACKUP_ROLE_KEY = "__backup__";
  public static final String BACKUP_EMOJI = "🛟";
  public static final String DUAL_BOX_ROLE_KEY = "__dual_box__";
  public static final String DUAL_BOX_EMOJI = "📦";
  public static final long BACKUP_PRIORITY_PENALTY = 100L;

  private final GuildMember guildMember;
  @Getter(AccessLevel.NONE)
  private final Set<String> roleKeys = new HashSet<>();

  public EventMemberSignup(GuildMember guildMember, String... roleKeys) {
    this.guildMember = guildMember;
    Collections.addAll(this.roleKeys, roleKeys);
  }

  public void addRoleKey(String roleKey) {
    roleKeys.add(roleKey);
  }

  public void addRole(de.qf0xb.raidsignupbot.domain.model.template.EventRole role) {
    addRoleKey(role.getKey());
  }

  public void removeRoleKey(String roleKey) {
    roleKeys.remove(roleKey);
  }

  public boolean wantsRole(String roleKey) {
    return roleKeys.contains(roleKey);
  }

  public boolean wantsBackup() {
    return wantsRole(BACKUP_ROLE_KEY);
  }

  public long getEffectivePriority() {
    return guildMember.getPriority() - (wantsBackup() ? BACKUP_PRIORITY_PENALTY : 0L);
  }

  public boolean wantsDualBox() {
    return wantsRole(DUAL_BOX_ROLE_KEY);
  }

  public int getMaxAssignments() {
    return wantsDualBox() ? 2 : 1;
  }

  public Set<String> getRoleKeys() {
    return Collections.unmodifiableSet(roleKeys);
  }
}
