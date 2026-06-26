package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.listener;

import jakarta.enterprise.context.ApplicationScoped;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.time.Instant;
import java.util.Objects;

@ApplicationScoped
public class RaidGuildJoinListener extends ListenerAdapter {
  @Override
  public void onGuildJoin(@NonNull GuildJoinEvent event) {

    EmbedBuilder embedBuilder = new EmbedBuilder();
    embedBuilder.setTitle("Welcome to RaidSignupBot!");
    embedBuilder.setTimestamp(Instant.now()).setFooter("Made by StormFox");

    StringBuilder joinMessage = new StringBuilder();
    joinMessage.append("Hi! I'm RaidSignupBot!").append("\n");
    joinMessage.append("I'm here to help you organize raids!").append("\n");
    joinMessage.append("We need to onboard your server!").append("\n");
    joinMessage.append("Please use `/onboard` to get started!").append("\n");

    embedBuilder.setDescription(joinMessage.toString());
    embedBuilder.setColor(Color.getColor("EFBF04"));

    Objects.requireNonNull(event.getGuild().getDefaultChannel()).asTextChannel().sendMessageEmbeds(embedBuilder.build()).queue();
  }
}
