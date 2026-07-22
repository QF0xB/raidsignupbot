package de.qf0xb.raidsignupbot.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Builder
@Getter
@AllArgsConstructor
public class UpdateGuildCommand {
  private String name;
  private String serverId;

  @Builder.Default private boolean raiderRolesEnabled = true;
  private String raiderRoleId;
  private String eventRoleId;
}
