package de.qf0xb.raidsignupbot.domain.model.guild;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** A raider role the bot added to a player for one raid; it is removed again once no raid needs it. */
@Getter
@Builder
public class RaiderRoleGrant {
  private final Long id;
  private final Long eventId;
  private final String serverId;
  private final String userId;
  private final String roleId;

  @Setter
  private boolean backup;

  @Setter
  private Instant releaseAfter;
}
