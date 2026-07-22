package de.qf0xb.raidsignupbot.domain.model.event;

import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Builder
@Getter
public class Event {
  private final Long id;

  private final Guild guild;
  private final GuildMember hoster;
  @Setter
  private GuildMember eventAdmin;

  @Setter
  private String categoryId;
  @Setter
  private String signUpChannelId;
  @Setter
  private String messageId;
  @Setter
  private String teamsMessageId;
  @Setter
  private String voiceChannelId;

  @Setter
  private int donation;

  @Setter
  private boolean donationPerWin = false;

  @Setter
  private Instant start;

  @Setter
  private Instant end;

  private final EventTemplate template;
  private final List<EventTeam> teams = new ArrayList<>();

  @Builder.Default
  @Setter
  private boolean signupOpen = true;

  @Builder.Default
  @Setter
  private boolean teamsGenerated = false;

  @Builder.Default
  @Setter
  private boolean channelsExist = false;

  @Builder.Default
  @Setter
  private boolean approvalPending = false;

  @Builder.Default
  @Setter
  private boolean oneHourBeforeEmitted = false;

  @Builder.Default
  @Setter
  private boolean thirtyMinutesBeforeEmitted = false;

  @Builder.Default
  @Setter
  private boolean fifteenMinutesBeforeEmitted = false;

  @Builder.Default
  @Setter
  private boolean oneHourAfterEmitted = false;

  @Builder.Default
  @Setter
  private boolean teamsRequireApproval = false;

  public void addTeam(EventTeam team) {
    teams.add(team);
  }

  public void removeTeam(EventTeam team) {
    teams.remove(team);
  }

  public void clearTeams() {
    teams.clear();
  }

  public void closeSignup() {
    signupOpen = false;
    teamsGenerated = true;
  }

  public void reopenSignup() {
    signupOpen = true;
    teamsGenerated = false;
    approvalPending = false;
  }

  public String getDuration() {
    Duration duration = Duration.between(start, end).abs();


    return switch((int) duration.toMinutes()) {
      case 60 -> "1h";
      case 90 -> "1,5h";
      case 120 -> "2h";
      case 180 -> "3h";
      default -> duration.toMinutes() + "m";
    };
  }

  public void markOneHourBeforeEmitted() {
    oneHourBeforeEmitted = true;
  }

  public void markThirtyMinutesBeforeEmitted() {
    thirtyMinutesBeforeEmitted = true;
  }

  public void markFifteenMinutesBeforeEmitted() {
    fifteenMinutesBeforeEmitted = true;
  }

  public void markOneHourAfterEmitted() {
    oneHourAfterEmitted = true;
  }

  public void markChannelsExist() {
    channelsExist = true;
  }

  public void markApprovalPending() {
    approvalPending = true;
  }

  public void clearApprovalPending() {
    approvalPending = false;
  }
}
