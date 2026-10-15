package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.util;

import de.qf0xb.raidsignupbot.application.service.guild.GuildMemberService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CollectorTest {
  @Test
  @SuppressWarnings("unchecked")
  void collectsDualBoxAndBackupFlagsOnTheSameSignupAndIgnoresTheBot() {
    Collector collector = new Collector();
    collector.api = mock(JDA.class);
    collector.discordEmojiResolver = mock(DiscordEmojiResolver.class);
    when(collector.discordEmojiResolver.resolve(anyString()))
        .thenAnswer(invocation -> Emoji.fromUnicode(invocation.getArgument(0)));
    collector.guildMemberService = mock(GuildMemberService.class);
    when(collector.guildMemberService.ensureGuildMember(anyString(), anyString()))
        .thenAnswer(invocation -> new GuildMember(invocation.getArgument(0)));
    SelfUser bot = mock(SelfUser.class);
    when(bot.getId()).thenReturn("bot");
    when(collector.api.getSelfUser()).thenReturn(bot);
    TextChannel channel = mock(TextChannel.class);
    Message message = mock(Message.class);
    RestAction<Message> retrieve = mock(RestAction.class);
    when(collector.api.getTextChannelById("channel")).thenReturn(channel);
    when(channel.retrieveMessageById("signup")).thenReturn(retrieve);
    when(retrieve.complete()).thenReturn(message);
    User dual = user("dual");
    User single = user("single");
    reactions(message, "🔥", List.of(dual, single, bot));
    reactions(message, EventMemberSignup.BACKUP_EMOJI, List.of(dual, bot));
    reactions(message, EventMemberSignup.DUAL_BOX_EMOJI, List.of(dual, bot));
    EventStrategy strategy = new EventStrategy("main", "Main");
    strategy.addRole(new EventRole("fire", "Fire", "🔥", 1));
    EventTemplate template = new EventTemplate("raid", "Raid", "", "coin");
    template.addStrategy(strategy);
    Event event = Event.builder().guild(new Guild("Guild", "server")).template(template)
        .signUpChannelId("channel").messageId("signup").build();

    List<EventMemberSignup> signups = collector.collect(event);

    assertEquals(2, signups.size());
    EventMemberSignup dualSignup = signups.stream()
        .filter(signup -> signup.getGuildMember().getUserId().equals("dual")).findFirst().orElseThrow();
    assertEquals(Set.of("fire", EventMemberSignup.BACKUP_ROLE_KEY, EventMemberSignup.DUAL_BOX_ROLE_KEY),
        dualSignup.getRoleKeys());
    assertTrue(dualSignup.wantsDualBox());
    assertTrue(dualSignup.wantsBackup());
    EventMemberSignup singleSignup = signups.stream()
        .filter(signup -> signup.getGuildMember().getUserId().equals("single")).findFirst().orElseThrow();
    assertFalse(singleSignup.wantsDualBox());
    assertEquals(1, singleSignup.getMaxAssignments());
  }

  private void reactions(Message message, String emoji, List<User> users) {
    ReactionPaginationAction action = mock(ReactionPaginationAction.class);
    when(message.retrieveReactionUsers(Emoji.fromUnicode(emoji))).thenReturn(action);
    when(action.complete()).thenReturn(users);
  }

  private User user(String id) {
    User user = mock(User.class);
    when(user.getId()).thenReturn(id);
    return user;
  }
}
