package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord;

import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.manager.CommandManager;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.manager.ComponentManager;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;

@Slf4j
@ApplicationScoped
public class DiscordBot {
  @Inject
  JDA discordApi;

  @Inject
  CommandManager commandManager;

  @Inject
  ComponentManager componentManager;

  void onStart(@Observes StartupEvent ev) {
    log.info("Starting Discord bot bootstrap");
    discordApi.addEventListener(commandManager, componentManager);
    commandManager.registerCommands();
    log.info("Discord bot started");
  }

  public boolean isReady() {
    return discordApi != null;
  }

  public JDA requireApi() {
    if (discordApi == null) {
      throw new IllegalStateException("Discord API is not initialized.");
    }

    return discordApi;
  }
}
