package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord;

import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import jakarta.enterprise.context.ApplicationScoped;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DiscordSignupCollector {
  private final DiscordBot bot;
  private final DiscordEmojiResolver emojiResolver;

  public DiscordSignupCollector(DiscordBot bot, DiscordEmojiResolver emojiResolver) {
    this.bot = bot;
    this.emojiResolver = emojiResolver;
  }

  public List<EventMemberSignup> collect(Event event) {
    TextChannel channel = bot.requireApi().getTextChannelById(event.getSignUpChannelId());
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

  private void collectRoleReactions(
      Message signUp,
      Map<String, EventMemberSignup> signupsByUserId,
      EventRole role) {
    signUp.retrieveReactionUsers(emojiResolver.resolve(role.getEmojiSlug()))
        .complete()
        .stream()
        .map(user -> user.getId())
        .filter(userId -> !bot.requireApi().getSelfUser().getId().equals(userId))
        .forEach(userId -> {
          EventMemberSignup signup =
              signupsByUserId.computeIfAbsent(
                  userId,
                  ignored -> new EventMemberSignup(new GuildMember(userId)));
          signup.addRoleKey(role.getKey());
        });
  }
}
