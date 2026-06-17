package de.qf0xb.raidsignupbot.signup.domain.model;

import de.qf0xb.raidsignupbot.template.domain.model.EventTemplate;

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
  private final List<EventPlayerSignup> unmatchedPlayers = new ArrayList<>();

  private EventTemplate template;

  public Event(String name, String guildId, String channelCategoryId, String hosterId, Instant startTime, Instant endTime, float donation, EventTemplate template) {
    this.name = name;
    this.guildId = guildId;
    this.channelCategoryId = channelCategoryId;
    this.hosterId = hosterId;
    this.startTime = startTime;
    this.endTime = endTime;
    this.donation = donation;
    this.template = template;

    signup = new EventSignup("Test");
  }

  public EventTemplate getTemplate() {
    return template;
  }

  public void setTemplate(EventTemplate template) {
    this.template = template;
  }

  public EventSignup getSignup() {
    return signup;
  }

  public List<EventTeam> getRoster() {
    return roster;
  }

  public void addTeam(EventTeam team) {
    roster.add(team);
  }

  public void removeTeam(EventTeam team) {
    roster.remove(team);
  }

  public void resetRoster() {
    roster.clear();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getGuildId() {
    return guildId;
  }

  public void setGuildId(String guildId) {
    this.guildId = guildId;
  }

  public String getChannelCategoryId() {
    return channelCategoryId;
  }

  public void setChannelCategoryId(String channelCategoryId) {
    this.channelCategoryId = channelCategoryId;
  }

  public String getHosterId() {
    return hosterId;
  }

  public void setHosterId(String hosterId) {
    this.hosterId = hosterId;
  }

  public float getDonation() {
    return donation;
  }

  public void setDonation(float donation) {
    this.donation = donation;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public void setStartTime(Instant startTime) {
    this.startTime = startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }

  public void setEndTime(Instant endTime) {
    this.endTime = endTime;
  }

  public List<EventPlayerSignup> getUnmatchedPlayers() {
    return unmatchedPlayers;
  }

  public void addUnmatchedPlayer(EventPlayerSignup player) {
    unmatchedPlayers.add(player);
  }

  public void addUnmatchedPlayers(List<EventPlayerSignup> players) {
    unmatchedPlayers.addAll(players);
  }

  public void removeUnmatchedPlayer(EventPlayerSignup player) {
    unmatchedPlayers.remove(player);
  }
}
