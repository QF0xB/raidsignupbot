package de.qf0xb.raidsignupbot.application.port.guild;

/** Reads and changes a member's roles on the chat platform. */
public interface MemberRolePort {
  boolean hasRole(String serverId, String userId, String roleId);

  void addRole(String serverId, String userId, String roleId);

  void removeRole(String serverId, String userId, String roleId);
}
