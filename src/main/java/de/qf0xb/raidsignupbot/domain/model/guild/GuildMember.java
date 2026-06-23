package de.qf0xb.raidsignupbot.domain.model.guild;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Represents a member of a guild, encapsulating their unique identifier, a list of active guilds
 * they are associated with, and a priority value. This class provides utilities for managing
 * the member's active guilds and priority.
 */
@Getter
@Setter
@EqualsAndHashCode(of = "userId")
@ToString
public class GuildMember {
  private final String userId;

  @Getter(AccessLevel.NONE)
  private final List<Guild> activeGuilds = new ArrayList<>();

  private long priority = 0L;

  public GuildMember(String userId) {
    this.userId = userId;
  }

  public void addActiveGuild(Guild guild) {
    activeGuilds.add(guild);
  }

  public void removeActiveGuild(Guild guild) {
    activeGuilds.remove(guild);
  }

  public void increasePriority() {
    priority++;
  }

  public void assignedRole() {
    priority = 0;
  }

  public List<Guild> getActiveGuilds() {
    return Collections.unmodifiableList(activeGuilds);
  }
}
