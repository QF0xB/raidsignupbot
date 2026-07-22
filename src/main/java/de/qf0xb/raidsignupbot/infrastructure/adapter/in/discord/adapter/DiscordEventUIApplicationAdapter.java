package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.adapter;

import de.qf0xb.raidsignupbot.application.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.application.port.EventUIApplicationPort;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.event.EventSlot;
import de.qf0xb.raidsignupbot.domain.model.event.EventUtilityAssignment;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.util.MessageFactory;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.util.DiscordEmojiResolver;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.util.Collector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@ApplicationScoped
public class DiscordEventUIApplicationAdapter implements EventUIApplicationPort {

  @Inject
  JDA api;

  @Inject
  MessageFactory messageFactory;

  @Inject
  DiscordEmojiResolver emojiResolver;

  @Inject
  Collector signupCollector;

  @Inject
  ServerService serverService;

  @Override
  public void addEvent(Event event) {
    Server server = serverService.findServerById(event.getGuild().getServerId());
    if (needsGeneratedChannels(server)) {
      createChannels(event);
      event.markChannelsExist();
    }

    postSignup(event);
  }

  @Override
  public void updateEvent(Event existingEvent, Event updatedEvent) {
    updatedEvent.setCategoryId(existingEvent.getCategoryId());
    updatedEvent.setSignUpChannelId(existingEvent.getSignUpChannelId());
    updatedEvent.setVoiceChannelId(existingEvent.getVoiceChannelId());
    updatedEvent.setMessageId(existingEvent.getMessageId());
    updatedEvent.setTeamsMessageId(existingEvent.getTeamsMessageId());
    updatedEvent.setChannelsExist(existingEvent.isChannelsExist());

    Server server = serverService.findServerById(updatedEvent.getGuild().getServerId());
    if (needsGeneratedChannels(server)) {
      if (!updatedEvent.isChannelsExist()) {
        createChannels(updatedEvent);
        updatedEvent.markChannelsExist();
      }
    }

    if (updatedEvent.getMessageId() != null && updatedEvent.getSignUpChannelId() != null) {
      updateSignupMessage(updatedEvent);
    } else {
      postSignup(updatedEvent);
    }
  }

  private void postSignup(Event event) {
    TextChannel textChannel = api.getTextChannelById(event.getSignUpChannelId());
    if (textChannel == null) {
      throw new IllegalStateException(String.format("TextChannel %s of Event %s does not exist.", event.getSignUpChannelId(), event.getId()));
    }

    String ping = messageFactory.buildSignupPing(event);
    Message message = ping.isBlank()
        ? textChannel.sendMessageEmbeds(messageFactory.buildSignupEmbed(event).build()).complete()
        : textChannel.sendMessage(ping)
            .setEmbeds(messageFactory.buildSignupEmbed(event).build())
            .complete();

    event.setMessageId(message.getId());

    log.debug("Message was posted to TextChannel {} of Event {} | {}", event.getSignUpChannelId(), event.getId(), event.getTemplate().getKey());

    addReactions(event, message);
  }

  private void createChannels(Event event) {
    Server server = serverService.findServerById(event.getGuild().getServerId());
    Category category = api.getCategoryById(event.getCategoryId());
    if (category == null) {
      throw new IllegalStateException(String.format("Category %s of Event %s does not exist.", event.getCategoryId(), event.getId()));
    }

    String day = event.getStart().atZone(ZoneId.systemDefault()).getDayOfWeek().name().substring(0, 3).toLowerCase();
    String name = (event.getTemplate().getKey() + "-" + day).toLowerCase().strip();

    if (server.isManageSignupChannels()) {
      TextChannel textChannel = category.createTextChannel(name)
          .complete();
      event.setSignUpChannelId(textChannel.getId());

      log.debug("TextChannel {} was created for Event {} | {}, with ID {}", name, event.getId(), event.getTemplate().getKey(), event.getSignUpChannelId());
    }

    if (server.isManageVoiceChannels()) {
      VoiceChannel voiceChannel = category.createVoiceChannel(name)
          .complete();
      event.setVoiceChannelId(voiceChannel.getId());

      log.debug("VoiceChannel {} was created for Event {} | {}, with ID {}", name, event.getId(), event.getTemplate().getKey(), event.getVoiceChannelId());
    }
  }

  private void addReactions(Event event, Message message) {
    for (EventStrategy strat : event.getTemplate().getStrategies()) {
      for (EventRole role : strat.getPrimaryRoles()) {
        message.addReaction(emojiResolver.resolve(role.getEmojiSlug())).queue();
      }

      for (EventUtilityRole role : strat.getUtilityRoles()) {
        message.addReaction(emojiResolver.resolve(role.getEmojiSlug())).queue();
      }
    }
  }

  private void updateSignupMessage(Event event) {
    TextChannel textChannel = api.getTextChannelById(event.getSignUpChannelId());
    if (textChannel == null) {
      throw new IllegalStateException(String.format("TextChannel %s of Event %s does not exist.", event.getSignUpChannelId(), event.getId()));
    }

    String ping = messageFactory.buildSignupPing(event);
    textChannel.retrieveMessageById(event.getMessageId())
        .queue(message -> message.editMessage(ping)
            .setEmbeds(messageFactory.buildSignupEmbed(event).build())
            .queue());
  }

  @Override
  public List<EventMemberSignup> closeSignup(Event event) {
    return signupCollector.collect(event);
  }

  @Override
  public void removeSignup(Event event, String userId) {
    TextChannel textChannel = api.getTextChannelById(event.getSignUpChannelId());
    if (textChannel == null) {
      throw new IllegalStateException(String.format("TextChannel %s of Event %s does not exist.", event.getSignUpChannelId(), event.getId()));
    }

    Message signUpMessage = textChannel.retrieveMessageById(event.getMessageId()).complete();
    User user = api.retrieveUserById(userId).complete();

    for (EventStrategy strategy : event.getTemplate().getStrategies()) {
      for (EventRole role : strategy.getPrimaryRoles()) {
        removeReactionIfPresent(signUpMessage, role.getEmojiSlug(), user);
      }

      for (EventUtilityRole role : strategy.getUtilityRoles()) {
        removeReactionIfPresent(signUpMessage, role.getEmojiSlug(), user);
      }
    }
  }

  @Override
  public void publishTeams(Event event, TeamGeneratorResult result) {
    TextChannel textChannel = api.getTextChannelById(event.getSignUpChannelId());
    if (textChannel == null) {
      throw new IllegalStateException(String.format("TextChannel %s of Event %s does not exist.", event.getSignUpChannelId(), event.getId()));
    }

    String mentions = buildMentionList(result);
    EmbedBuilder embedBuilder = new EmbedBuilder();
    embedBuilder.setTitle(String.format("Event Teams for %s | %s", event.getTemplate().getName(), event.getGuild().getName()));
    embedBuilder.setDescription(messageFactory.buildTeams(event, result));
    embedBuilder.setColor(result.teams().isEmpty() ? 0xFF0000 : 0x00FF00);
    embedBuilder.setFooter("Teams published by RaidSignupBot | made by StormFox");

    if (mentions.isBlank()) {
      Message message = textChannel.sendMessageEmbeds(embedBuilder.build()).complete();
      event.setTeamsMessageId(message.getId());
    } else {
      Message message = textChannel.sendMessage(mentions)
          .setEmbeds(embedBuilder.build())
          .complete();
      event.setTeamsMessageId(message.getId());
    }
  }

  @Override
  public void updateTeams(Event event, TeamGeneratorResult result) {
    TextChannel textChannel = api.getTextChannelById(event.getSignUpChannelId());
    if (textChannel == null) {
      throw new IllegalStateException(String.format("TextChannel %s of Event %s does not exist.", event.getSignUpChannelId(), event.getId()));
    }

    EmbedBuilder embedBuilder = new EmbedBuilder();
    embedBuilder.setTitle(String.format("Event Teams for %s | %s", event.getTemplate().getName(), event.getGuild().getName()));
    embedBuilder.setDescription(messageFactory.buildTeams(event, result));
    embedBuilder.setColor(result.teams().isEmpty() ? 0xFF0000 : 0x00FF00);
    embedBuilder.setFooter("Teams published by RaidSignupBot | made by StormFox");

    if (event.getTeamsMessageId() != null) {
      Message message = textChannel.retrieveMessageById(event.getTeamsMessageId()).complete();
      message.editMessageEmbeds(embedBuilder.build()).complete();
    } else {
      Message message = textChannel.sendMessageEmbeds(embedBuilder.build()).complete();
      event.setTeamsMessageId(message.getId());
    }
  }

  private String buildMentionList(TeamGeneratorResult result) {
    Set<String> userIds = new LinkedHashSet<>();

    result.teams().forEach(team -> {
      team.getSlots().stream()
          .filter(EventSlot::isAssigned)
          .map(EventSlot::getMember)
          .map(GuildMember::getUserId)
          .forEach(userIds::add);

      team.getUtilityAssignments().stream()
          .filter(EventUtilityAssignment::isAssigned)
          .map(EventUtilityAssignment::getMember)
          .map(GuildMember::getUserId)
          .forEach(userIds::add);
    });

    result.unmatchedSignups().stream()
        .map(signup -> signup.getGuildMember().getUserId())
        .forEach(userIds::add);

    return userIds.stream()
        .map(userId -> "<@" + userId + ">")
        .reduce((left, right) -> left + " " + right)
        .orElse("");
  }

  @Override
  public void deleteEvent(Event event) {
    if (needsGeneratedChannels(serverService.findServerById(event.getGuild().getServerId()))) {
      deleteChannels(event);
    }
  }

  private boolean needsGeneratedChannels(Server server) {
    return server.isManageSignupChannels() || server.isManageVoiceChannels();
  }

  private void deleteChannels(Event event) {
    Server server = serverService.findServerById(event.getGuild().getServerId());

    if (server.isManageSignupChannels() && event.getSignUpChannelId() != null) {
      TextChannel textChannel = api.getTextChannelById(event.getSignUpChannelId());
      if (textChannel == null) {
        throw new IllegalStateException(String.format("TextChannel %s of Event %s does not exist.", event.getSignUpChannelId(), event.getId()));
      }

      textChannel.delete().queue();
    }

    if (server.isManageVoiceChannels() && event.getVoiceChannelId() != null) {
      VoiceChannel voiceChannel = api.getVoiceChannelById(event.getVoiceChannelId());
      if (voiceChannel == null) {
        throw new IllegalStateException(String.format("VoiceChannel %s of Event %s does not exist.", event.getVoiceChannelId(), event.getId()));
      }

      voiceChannel.delete().queue();
    }
  }

  private void removeReactionIfPresent(Message signUpMessage, String emojiSlug, User user) {
    signUpMessage.retrieveReactionUsers(emojiResolver.resolve(emojiSlug))
        .complete()
        .stream()
        .filter(candidate -> candidate.getId().equals(user.getId()))
        .findFirst()
        .ifPresent(ignored -> signUpMessage.removeReaction(emojiResolver.resolve(emojiSlug), user).queue());
  }
}
