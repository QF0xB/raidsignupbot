package de.qf0xb.raidsignupbot.signup.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Event {
  // Created by persistence layer. Used for search
  private Long id;

  private String name;
  private String guildId;
  private String channelCategoryId;

  private String hosterId;
  private float donation;

  private Instant startTime;
  private Instant endTime;

  private EventSignup signup;
  private final List<EventTeam> roster = new ArrayList<>();

  public Event(String name, String guildId, String channelCategoryId, String hosterId, float donation, Instant startTime, Instant endTime) {

  }
}
