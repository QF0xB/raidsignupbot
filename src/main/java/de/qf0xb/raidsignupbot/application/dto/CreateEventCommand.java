package de.qf0xb.raidsignupbot.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
@Builder
@Getter
@AllArgsConstructor
public class CreateEventCommand {
  private String serverId;
  private Long calendarEntryId;
  private String creatorUserId;
  private String guildName;
  private String hosterUserId;
  private String eventAdminUserId;

  private String categoryId;
  private String signUpChannelId;
  private String voiceChannelId;

  @Builder.Default private int donation = 0;
  @Builder.Default private boolean donationPerWin = false;
  @Builder.Default private boolean teamsRequireApproval = false;
  private Instant start;
  private Instant end;
  private String templateKey;
}
