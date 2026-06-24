package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord;

import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.port.EventUIApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.event.EventSlot;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.util.DiscordEmojiResolver;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.DiscordSignupCollector;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

import java.awt.*;
import java.time.ZoneId;
import java.util.List;

@ApplicationScoped
@Slf4j
public class DiscordEventUIApplicationAdapter implements EventUIApplicationPort {
  private final DiscordBot bot;
  private final EventApplicationPort eventApplicationPort;
  private final DiscordEventMessageFactory messageFactory;
  private final DiscordEmojiResolver emojiResolver;
  private final DiscordSignupCollector signupCollector;

  public DiscordEventUIApplicationAdapter(
      DiscordBot bot,
      EventApplicationPort eventApplicationPort,
      DiscordEventMessageFactory messageFactory,
      DiscordEmojiResolver emojiResolver,
      DiscordSignupCollector signupCollector) {
    this.bot = bot;
    this.eventApplicationPort = eventApplicationPort;
    this.messageFactory = messageFactory;
    this.emojiResolver = emojiResolver;
    this.signupCollector = signupCollector;
  }

  @Override
  public void addEvent(Event event) {
    JDA api = bot.requireApi();
    log.info(event.getCategoryId());
    Category cat = api.getCategoryById(event.getCategoryId());

    if (cat == null) {
      throw new IllegalStateException(String.format("Category %s of Event %s does not exist.", event.getCategoryId(), event.getId()));
    }

    if (event.getGuild().isManageEventChannels()) {
      createChannels(event, cat);
    }

    postSignup(event);
    eventApplicationPort.save(event);
  }

  private void postSignup(Event event) {
    TextChannel textChannel = bot.requireApi().getTextChannelById(event.getSignUpChannelId());
    if (textChannel == null) {
      throw new IllegalStateException(String.format("TextChannel %s of Event %s does not exist.", event.getSignUpChannelId(), event.getId()));
    }

    Message message = textChannel.sendMessageEmbeds(
        messageFactory.buildSignupEmbed(event).build()
    ).complete();

    event.setMessageId(message.getId());

    log.info("Message was posted to TextChannel {} of Event {} | {}", event.getSignUpChannelId(), event.getId(), event.getTemplate().getKey());

    addReactions(event, message);
  }

  private void createChannels(Event event, Category cat) {
    String day = event.getStart().atZone(ZoneId.systemDefault()).getDayOfWeek().name().substring(0, 3).toLowerCase();
    String name = (event.getTemplate().getKey() + "-" + day).toLowerCase().strip();
    if (event.getSignUpChannelId() == null) {
      TextChannel tc = cat.createTextChannel(name)
          .complete();
      event.setSignUpChannelId(tc.getId());

      log.info(
          "TextChannel {} was created for Event {} | {}, with ID {}",
          name,
          event.getId(),
          event.getTemplate().getKey(),
          event.getSignUpChannelId());
    }

    if (event.getVoiceChannelId() == null) {
      VoiceChannel vc = cat.createVoiceChannel(name)
          .complete();
      event.setVoiceChannelId(vc.getId());

      log.info(
          "VoiceChannel {} was created for Event {} | {}, with ID {}",
          name,
          event.getId(),
          event.getTemplate().getKey(),
          event.getVoiceChannelId());
    }

    event.markChannelsExist();
  }

  @Override
  public List<EventMemberSignup> closeSignup(Event event) {
    return signupCollector.collect(event);
  }

  @Override
  public void publishTeams(Event event) {
    StringBuilder sb = new StringBuilder();

    int count = 1;
    for (EventTeam team : event.getTeams()) {
      sb.append(String.format("Team %s: %s Strategy", count, team.getStrategy().getName()));
      for(EventSlot slot : team.getSlots()) {
        sb.append(String.format("\n%s: <@%s>", slot.getRoleName(), slot.getMember().getUserId()));
      }
      sb.append("\n\n");
    }

    TextChannel textChannel = bot.requireApi().getTextChannelById(event.getSignUpChannelId());
    if (textChannel == null) {
      throw new IllegalStateException(String.format("TextChannel %s of Event %s does not exist.", event.getSignUpChannelId(), event.getId()));
    }

    EmbedBuilder embedBuilder = new EmbedBuilder();
    embedBuilder.setTitle(String.format("Event Teams for %s | %s", event.getTemplate().getName(), event.getGuild().getName()));
    embedBuilder.setDescription(sb.toString());
    embedBuilder.setColor(Color.BLUE);
    embedBuilder.setFooter("Teams published by RaidSignupBot | made by StormFox");
    embedBuilder.setDescription(sb.toString());
    textChannel.sendMessageEmbeds(embedBuilder.build()).queue();
  }

  @Override
  public void deleteEvent(Event event) {
    if (event.getGuild().isManageEventChannels()) {
      deleteChannels(event);
    }
  }

  private void deleteChannels(Event event) {
    TextChannel textChannel = bot.requireApi().getTextChannelById(event.getSignUpChannelId());
    if (textChannel == null) {
      throw new IllegalStateException(String.format("TextChannel %s of Event %s does not exist.", event.getSignUpChannelId(), event.getId()));
    }

    textChannel.delete().queue();

    VoiceChannel voiceChannel = bot.requireApi().getVoiceChannelById(event.getVoiceChannelId());
    if (voiceChannel == null) {
      throw new IllegalStateException(String.format("VoiceChannel %s of Event %s does not exist.", event.getVoiceChannelId(), event.getId()));
    }

    voiceChannel.delete().queue();
  }

  private void addReactions(Event event, Message message) {
    for (var strategy : event.getTemplate().getStrategies()) {
      for (var role : strategy.getPrimaryRoles()) {
        message.addReaction(emojiResolver.resolve(role.getEmojiSlug())).queue();
      }

      for (var role : strategy.getUtilityRoles()) {
        message.addReaction(emojiResolver.resolve(role.getEmojiSlug())).queue();
      }
    }
  }
}
