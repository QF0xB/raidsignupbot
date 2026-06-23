package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord;

import de.qf0xb.raidsignupbot.infrastructure.config.DiscordConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.jboss.logging.Logger;

import java.util.Objects;

@ApplicationScoped
public class DiscordBot {
  private static final Logger LOG = Logger.getLogger(DiscordBot.class);

  @Inject
  DiscordConfig discordConfig;

  @Inject
  CommandManager commandManager;

  @Getter
  private JDA api;

  public JDA requireApi() {
    return Objects.requireNonNull(api, "Discord API is not initialized.");
  }

  public boolean isReady() {
    return api != null;
  }

  void onStart(@Observes StartupEvent event) throws InterruptedException {
    LOG.info("Starting Discord bot bootstrap");
    if (discordConfig.getToken().isEmpty()) {
      LOG.error("Discord token is not configured, skipping Discord startup.");
      return;
    }

    api = JDABuilder.createDefault(discordConfig.getToken().orElseThrow())
        .addEventListeners(commandManager)
        .setStatus(OnlineStatus.ONLINE)
        .setActivity(Activity.playing("Planning Wizard101 Raids"))
        .build().awaitReady();

    commandManager.registerCommands(api);
  }

  void onStop(@Observes ShutdownEvent event) {
    if (api != null) {
      api.shutdownNow();
      LOG.info("Discord bot stopped");
    }
  }
}
