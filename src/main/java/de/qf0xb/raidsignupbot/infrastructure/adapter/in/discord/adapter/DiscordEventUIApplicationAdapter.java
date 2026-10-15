package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.adapter;

import de.qf0xb.raidsignupbot.application.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.application.port.EventUIApplicationPort;
import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.application.service.event.EventBackupService;
import de.qf0xb.raidsignupbot.application.service.event.EventLifecycleNotification;
import de.qf0xb.raidsignupbot.application.service.event.EventLifecycleStage;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventBackupOffer;
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
import jakarta.enterprise.event.Observes;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

  @Inject
  EventApplicationPort eventApplicationPort;

  @Inject
  EventBackupService eventBackupService;

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

    message.addReaction(emojiResolver.resolve(EventMemberSignup.BACKUP_EMOJI)).queue();

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
            .queue(ignored -> message.addReaction(emojiResolver.resolve(EventMemberSignup.BACKUP_EMOJI)).queue()));
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

    removeReactionIfPresent(signUpMessage, EventMemberSignup.BACKUP_EMOJI, user);

  }

  @Override
  public void publishTeams(Event event, TeamGeneratorResult result) {
    TextChannel textChannel = api.getTextChannelById(event.getSignUpChannelId());
    if (textChannel == null) {
      throw new IllegalStateException(String.format("TextChannel %s of Event %s does not exist.", event.getSignUpChannelId(), event.getId()));
    }

    String mentions = buildMentionList(result);
    EmbedBuilder embedBuilder = buildTeamsEmbed(event, result, "Teams published by onyxguildbot | made by StormFox");

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
  public void requestBackups(Event event, List<EventBackupOffer> offers) {
    Map<String, List<EventBackupOffer>> offersByUser = offers.stream()
        .collect(Collectors.groupingBy(EventBackupOffer::userId));
    for (List<EventBackupOffer> playerOffers : offersByUser.values()) {
      EventBackupOffer offer = playerOffers.getFirst();
      try {
        User user = api.retrieveUserById(offer.userId()).complete();
        user.openPrivateChannel().complete()
            .sendMessage(String.format(
                "You were not assigned to a team for **%s** (<t:%d:F>). Are you available as a backup?\nEligible roles: **%s**\nAccepting confirms you for the roles that still need a backup.",
                event.getTemplate().getName(),
                event.getStart().getEpochSecond(),
                playerOffers.stream().map(EventBackupOffer::roleName).distinct().sorted()
                    .collect(Collectors.joining(", "))))
            .setComponents(ActionRow.of(
                Button.success("backup-offer:" + offer.offerId() + ":accept", "Yes, I can backup"),
                Button.danger("backup-offer:" + offer.offerId() + ":decline", "No, I am unavailable")))
            .queue();
      } catch (RuntimeException ex) {
        log.warn("Could not send backup request to user {} for event {}", offer.userId(), event.getId(), ex);
      }
    }
  }

  @Override
  public void announceBackup(Event event, List<EventBackupOffer> acceptedOffers) {
    EventBackupOffer acceptedOffer = acceptedOffers.getFirst();
    String notice = String.format(
        "%s <@%s> is now the backup for **%s** in **%s** (<t:%d:F>).",
        EventMemberSignup.BACKUP_EMOJI,
        acceptedOffer.userId(),
        acceptedOffers.stream().map(EventBackupOffer::roleName).distinct().sorted()
            .collect(Collectors.joining(", ")),
        event.getTemplate().getName(),
        event.getStart().getEpochSecond());

    Set<String> organizerIds = new LinkedHashSet<>();
    organizerIds.add(event.getHoster().getUserId());
    if (event.getEventAdmin() != null) {
      organizerIds.add(event.getEventAdmin().getUserId());
    }
    for (String organizerId : organizerIds) {
      try {
        api.retrieveUserById(organizerId).complete()
            .openPrivateChannel().complete()
            .sendMessage(notice)
            .queue();
      } catch (RuntimeException ex) {
        log.warn("Could not notify organizer {} about backup for event {}", organizerId, event.getId(), ex);
      }
    }

    // Show the confirmed backups on the published teams message as well.
    if (event.getTeamsMessageId() != null) {
      try {
        updateTeams(event, new TeamGeneratorResult(event.getTeams(), List.of()));
      } catch (RuntimeException ex) {
        log.warn("Could not add backup to teams message of event {}", event.getId(), ex);
      }
    }
  }

  @Override
  public void announcePostponement(Event event, Instant previousStart) {
    TextChannel channel = api.getTextChannelById(event.getSignUpChannelId());
    if (channel == null) {
      throw new IllegalStateException("The raid signup channel does not exist.");
    }
    channel.sendMessage(String.format(
        "🕒 **%s** has been postponed from <t:%d:F> to **<t:%d:F>** (<t:%d:R>). "
            + "Signups and teams are preserved.",
        event.getTemplate().getName(), previousStart.getEpochSecond(),
        event.getStart().getEpochSecond(), event.getStart().getEpochSecond())).queue();
  }

  @Override
  public void sendTeamsForApproval(Event event, TeamGeneratorResult result) {
    User eventAdmin = api.retrieveUserById(event.getEventAdmin().getUserId()).complete();
    EmbedBuilder embedBuilder = buildTeamsEmbed(event, result, "Teams awaiting approval by onyxguildbot | made by StormFox");
    embedBuilder.setDescription(messageFactory.buildTeams(event, result) + "\n\nApproval required before publishing.");

    ActionRow actions = ActionRow.of(
        Button.success("event-approval:" + event.getId() + ":approve", "Approve"),
        Button.danger("event-approval:" + event.getId() + ":reject", "Reject"),
        Button.secondary("event-approval:" + event.getId() + ":swap", "Swap members")
    );

    try {
      eventAdmin.openPrivateChannel().complete()
          .sendMessageEmbeds(embedBuilder.build())
          .setComponents(actions)
          .complete();
    } catch (RuntimeException ex) {
      log.warn("Failed to DM event admin {} for approval of event {}", event.getEventAdmin().getUserId(), event.getId(), ex);
      TextChannel textChannel = api.getTextChannelById(event.getSignUpChannelId());
      if (textChannel == null) {
        throw new IllegalStateException("Unable to request approval because no signup channel exists for event " + event.getId(), ex);
      }

      textChannel.sendMessage(eventAdmin.getAsMention())
          .setEmbeds(embedBuilder.build())
          .setComponents(ActionRow.of(
              Button.success("event-approval:" + event.getId() + ":approve", "Approve"),
              Button.danger("event-approval:" + event.getId() + ":reject", "Reject"),
              Button.secondary("event-approval:" + event.getId() + ":swap", "Swap members")
          ))
          .complete();
    }
  }

  @Override
  public void updateTeams(Event event, TeamGeneratorResult result) {
    TextChannel textChannel = api.getTextChannelById(event.getSignUpChannelId());
    if (textChannel == null) {
      throw new IllegalStateException(String.format("TextChannel %s of Event %s does not exist.", event.getSignUpChannelId(), event.getId()));
    }

    EmbedBuilder embedBuilder = buildTeamsEmbed(event, result, "Teams published by onyxguildbot | made by StormFox");

    if (event.getTeamsMessageId() != null) {
      Message message = textChannel.retrieveMessageById(event.getTeamsMessageId()).complete();
      message.editMessageEmbeds(embedBuilder.build()).complete();
    } else {
      Message message = textChannel.sendMessageEmbeds(embedBuilder.build()).complete();
      event.setTeamsMessageId(message.getId());
    }
  }

  private EmbedBuilder buildTeamsEmbed(Event event, TeamGeneratorResult result, String footer) {
    EmbedBuilder embedBuilder = new EmbedBuilder();
    embedBuilder.setTitle(String.format("Event Teams for %s | %s", event.getTemplate().getName(), event.getGuild().getName()));
    embedBuilder.setDescription(messageFactory.buildTeams(event, result));
    embedBuilder.setColor(result.teams().isEmpty() ? 0xFF0000 : 0x00FF00);
    embedBuilder.setFooter(footer);
    if (event.getId() != null) {
      String backups = messageFactory.buildAcceptedBackups(eventBackupService.findAcceptedOffers(event.getId()));
      if (!backups.isEmpty()) {
        embedBuilder.addField(EventMemberSignup.BACKUP_EMOJI + " Backups", backups, false);
      }
    }
    return embedBuilder;
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

  @Override
  public void removeEventFromDiscord(Event event) {
    Server server = serverService.findServerById(event.getGuild().getServerId());
    if (server.isManageSignupChannels()) {
      deleteChannelQuietly(event, event.getSignUpChannelId());
    } else {
      deleteMessageQuietly(event, event.getSignUpChannelId(), event.getMessageId());
      deleteMessageQuietly(event, event.getSignUpChannelId(), event.getTeamsMessageId());
    }
    if (server.isManageVoiceChannels()) {
      deleteChannelQuietly(event, event.getVoiceChannelId());
    }
  }

  private void deleteChannelQuietly(Event event, String channelId) {
    if (channelId == null) {
      return;
    }
    try {
      var channel = api.getGuildChannelById(channelId);
      if (channel != null) {
        channel.delete().complete();
      }
    } catch (RuntimeException ex) {
      log.warn("Could not delete channel {} of event {}", channelId, event.getId(), ex);
    }
  }

  private void deleteMessageQuietly(Event event, String channelId, String messageId) {
    if (channelId == null || messageId == null) {
      return;
    }
    try {
      TextChannel channel = api.getTextChannelById(channelId);
      if (channel != null) {
        channel.deleteMessageById(messageId).complete();
      }
    } catch (RuntimeException ex) {
      log.warn("Could not delete message {} of event {}", messageId, event.getId(), ex);
    }
  }

  public void onEventLifecycleNotification(@Observes EventLifecycleNotification notification) {
    eventApplicationPort.findById(notification.eventId())
        .ifPresentOrElse(
            event -> sendReminder(event, notification.stage()),
            () -> log.warn("Skipping lifecycle notification for missing event id {}", notification.eventId()));
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

  public void sendReminder(Event event, EventLifecycleStage stage) {
    TextChannel textChannel = api.getTextChannelById(event.getSignUpChannelId());
    if (textChannel == null) {
      throw new IllegalStateException(String.format("TextChannel %s of Event %s does not exist.", event.getSignUpChannelId(), event.getId()));
    }

    List<EventMemberSignup> assignedPlayers = collectAssignedPlayers(event);
    if (assignedPlayers.isEmpty()) {
      log.info("Skipping {} reminder for event {} because there are no assigned players", stage, event.getId());
      return;
    }

    String mentions = switch (stage) {
      case ONE_HOUR_BEFORE, THIRTY_MINUTES_BEFORE -> messageFactory.buildMentionList(assignedPlayers);
      case FIFTEEN_MINUTES_BEFORE -> messageFactory.buildMentionList(findMissingSignups(event, assignedPlayers));
      case ONE_HOUR_AFTER -> "";
    };

    var reminderEmbed = messageFactory.buildReminderEmbed(event, stage).build();
    if (mentions.isBlank()) {
      textChannel.sendMessageEmbeds(reminderEmbed).queue();
    } else {
      textChannel.sendMessage(mentions)
          .setEmbeds(reminderEmbed)
          .queue();
    }
  }

  private List<EventMemberSignup> collectAssignedPlayers(Event event) {
    List<EventMemberSignup> signups = signupCollector.collect(event);
    return signups.stream()
        .filter(signup -> event.getTeams().stream().anyMatch(team -> team.containsMember(signup.getGuildMember())))
        .toList();
  }

  private List<EventMemberSignup> findMissingSignups(Event event, List<EventMemberSignup> assignedPlayers) {
    if (event.getVoiceChannelId() == null || event.getVoiceChannelId().isBlank()) {
      return assignedPlayers;
    }

    VoiceChannel voiceChannel = api.getVoiceChannelById(event.getVoiceChannelId());
    if (voiceChannel == null) {
      throw new IllegalStateException(String.format("VoiceChannel %s of Event %s does not exist.", event.getVoiceChannelId(), event.getId()));
    }

    Set<String> voiceUserIds = voiceChannel.getMembers().stream()
        .map(Member::getId)
        .collect(Collectors.toCollection(HashSet::new));

    return assignedPlayers.stream()
        .filter(signup -> !voiceUserIds.contains(signup.getGuildMember().getUserId()))
        .toList();
  }
}
