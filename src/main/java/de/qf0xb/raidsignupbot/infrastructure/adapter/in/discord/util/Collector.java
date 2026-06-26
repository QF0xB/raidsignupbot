package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.util;

import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.application.port.guild.GuildMemberApplicationPort;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.util.MessageFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class Collector {
  @Inject
  JDA api;

  @Inject
  MessageFactory messageFactory;
  @Inject
  DiscordEmojiResolver discordEmojiResolver;

  @Inject
  GuildMemberApplicationPort guildMemberApplicationPort;

  public List<EventMemberSignup> collect(Event event) {
    TextChannel channel = api.getTextChannelById(event.getSignUpChannelId());
    if (channel == null) {
      throw new IllegalStateException(String.format("TextChannel %s of Event %s does not exist.", event.getSignUpChannelId(), event.getId()));
    }

    Message signUp = channel.retrieveMessageById(event.getMessageId()).complete();
    Map<String, EventMemberSignup> signupsByUserId = new LinkedHashMap<>();

    for (EventStrategy strategy : event.getTemplate().getStrategies()) {
      for (EventRole role : strategy.getPrimaryRoles()) {
        collectRoleReactions(signUp, signupsByUserId, role);
      }

      for (EventRole role : strategy.getUtilityRoles()) {
        collectRoleReactions(signUp, signupsByUserId, role);
      }
    }

    return new ArrayList<>(signupsByUserId.values());
  }

  private void collectRoleReactions(Message signUp, Map<String, EventMemberSignup> signupsByUserId, EventRole role) {
    signUp.retrieveReactionUsers(discordEmojiResolver.resolve(role.getEmojiSlug()))
        .complete()
        .stream()
        .map(User::getId)
        .filter(userId -> !api.getSelfUser().getId().equals(userId))
        .forEach(userId -> guildMemberApplicationPort.findByUserId(userId)
            .ifPresent(guildMember -> {
              EventMemberSignup signup =
                  signupsByUserId.computeIfAbsent(
                      userId,
                      ignored -> new EventMemberSignup(guildMember)
                  );
              signup.addRoleKey(role.getKey());
            }));
  }
}
