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

  public Set<String> getRoleKeys() {
    return Collections.unmodifiableSet(roleKeys);
  }
}
