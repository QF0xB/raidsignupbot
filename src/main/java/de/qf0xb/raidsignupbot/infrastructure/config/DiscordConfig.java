package de.qf0xb.raidsignupbot.infrastructure.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ApplicationScoped
public class DiscordConfig {
  @ConfigProperty(name = "discord.token")
  Optional<String> token;

  @ConfigProperty(name = "discord.application-id")
  Optional<String> applicationId;

  @ConfigProperty(name = "discord.dev.guild-id")
  Optional<String> guildId;

  public Optional<String> getToken() {
    return token == null ? Optional.empty() : token.filter(value -> !value.isBlank());
  }

  public Optional<String> getApplicationId() {
    return applicationId == null ? Optional.empty() : applicationId.filter(value -> !value.isBlank());
  }

  public Optional<String> getGuildId() {
    return guildId == null ? Optional.empty() : guildId.filter(value -> !value.isBlank());
  }
}
