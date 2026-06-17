package de.qf0xb.raidsignupbot.signup.domain.service;

import java.util.ArrayList;
import java.util.List;

public class EventSignup {
  private final Long id;
  private final String guildId;
  private final String channelId;
  private final String messageId;
  private String hosterUserId;
  private EventTemplate template;
  private final List<PlayerSignup> playerSignups = new ArrayList<>();

  public EventSignup(Long id, String guildId, String channelId, String messageId) {
    this.id = id;
    this.guildId = guildId;
    this.channelId = channelId;
    this.messageId = messageId;
  }

  public EventSignup(Long id, String guildId, String channelId, String messageId, String hosterUserId) {
    this(id, guildId, channelId, messageId);
    this.hosterUserId = hosterUserId;
  }

  public Long getId() {
    return id;
  }

  public String getGuildId() {
    return guildId;
  }

  public String getChannelId() {
    return channelId;
  }

  public String getMessageId() {
    return messageId;
  }

  public String getHosterUserId() {
    return hosterUserId;
  }

  public void setHosterUserId(String hosterUserId) {
    this.hosterUserId = hosterUserId;
  }

  public EventTemplate getTemplate() {
    return template;
  }

  public void addTemplate(EventTemplate template) {
    this.template = template;
  }

  public List<PlayerSignup> getPlayerSignups() {
    return playerSignups;
  }

  public void addOrUpdatePlayerSignup(PlayerSignup playerSignup) {
    removePlayerSignup(playerSignup.getUserId());
    playerSignups.add(playerSignup);
  }

  public void removePlayerSignup(String userId) {
    playerSignups.removeIf(playerSignup -> playerSignup.getUserId().equals(userId));
  }
}
