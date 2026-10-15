package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord;

import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.manager.CommandManager;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.manager.ComponentManager;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.listener.GuildRoleSyncListener;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.listener.RaidGuildJoinListener;
import de.qf0xb.raidsignupbot.application.service.guild.GuildMemberSyncService;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Slf4j
@ApplicationScoped
public class DiscordBot {
  @Inject
  JDA discordApi;

  @Inject
  CommandManager commandManager;

  @Inject
  ComponentManager componentManager;

  @Inject
  RaidGuildJoinListener raidGuildJoinListener;

  @Inject
  GuildRoleSyncListener guildRoleSyncListener;

  @Inject
  GuildMemberSyncService guildMemberSyncService;

  @ConfigProperty(name = "discord.bot.enabled", defaultValue = "true")
  boolean enabled;

  void onStart(@Observes StartupEvent ev) {
    if (!enabled) {
      log.info("Discord bot disabled; skipping bootstrap");
      return;
    }
    log.info("Starting Discord bot bootstrap");
    discordApi.addEventListener(commandManager, componentManager, raidGuildJoinListener, guildRoleSyncListener);
    commandManager.registerCommands();
    guildMemberSyncService.syncAllKnownGuilds();
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
