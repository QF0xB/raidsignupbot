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
  private String guildId;
  private String hosterUserId;

  private String categoryId;

  @Builder.Default private int donation = 0;
  @Builder.Default private boolean donationPerWin = false;
  private Instant start;
  private Instant end;
  private String templateKey;
}
