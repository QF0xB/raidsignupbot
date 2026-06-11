package de.qf0xb.raidsignupbot.signup.domain.model;

public class EventSignup {
  private final String discordChannel;

  public EventSignup(String discordChannel) {
    this.discordChannel = discordChannel;
  }

  public void closeSignup() {
    // TODO
  }

  public String getDiscordChannel() {
    return discordChannel;
  }
}
