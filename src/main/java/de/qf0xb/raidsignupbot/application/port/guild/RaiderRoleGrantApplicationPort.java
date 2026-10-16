package de.qf0xb.raidsignupbot.application.port.guild;

import de.qf0xb.raidsignupbot.domain.model.guild.RaiderRoleGrant;

import java.time.Instant;
import java.util.List;

public interface RaiderRoleGrantApplicationPort {
  List<RaiderRoleGrant> findByEventId(Long eventId);

  List<RaiderRoleGrant> findReleasableBefore(Instant now);

  long countGrants(String serverId, String userId, String roleId);

  RaiderRoleGrant save(RaiderRoleGrant grant);

  void deleteById(Long id);
}
