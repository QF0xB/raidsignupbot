package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.adapter;

import de.qf0xb.raidsignupbot.infrastructure.config.DiscordConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.ChunkingFilter;

@ApplicationScoped
public class DiscordClientProducer {
  @Produces
  @ApplicationScoped
  public JDA produceJda(DiscordConfig discordConfig) throws InterruptedException {
    if (discordConfig.getToken().isEmpty()) {
      throw new IllegalStateException("Discord token is not configured.");
    }

    return JDABuilder.createDefault(discordConfig.getToken().orElseThrow())
        .enableIntents(GatewayIntent.GUILD_MEMBERS)
        .setMemberCachePolicy(MemberCachePolicy.ALL)
        .setChunkingFilter(ChunkingFilter.ALL)
        .setStatus(OnlineStatus.ONLINE)
        .setActivity(Activity.playing("Planning Wizard101 Activities"))
        .build()
        .awaitReady();
  }
}
