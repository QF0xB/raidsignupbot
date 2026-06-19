package de.qf0xb.raidsignupbot.domain.model.event;

import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
public class Event {
  private final Long id;

  private final Guild guild;
  private final GuildMember hoster;

  @Setter
  private int donation;

  @Setter
  private Instant start;

  @Setter
  private Instant end;

  private final EventTemplate template;
  private final List<EventTeam> teams = new ArrayList<>();

  @Builder.Default
  private boolean signupOpen = true;

  public void addTeam(EventTeam team) {
    teams.add(team);
  }

  public void removeTeam(EventTeam team) {
    teams.remove(team);
  }

  public void closeSignup() {
    signupOpen = false;
  }
}
