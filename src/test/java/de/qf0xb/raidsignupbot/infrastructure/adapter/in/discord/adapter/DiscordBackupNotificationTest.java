package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.adapter;

import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventBackupOffer;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiscordBackupNotificationTest {
  @Test
  void sendsOneRequestPerPlayerWithAllEligibleRoles() {
    DiscordEventUIApplicationAdapter adapter = adapter();
    PrivateChannel playerChannel = prepareChannel(adapter, "player");
    PrivateChannel otherChannel = prepareChannel(adapter, "other");
    adapter.requestBackups(event(), List.of(
        offer("1", "player", "fire", "Fire"),
        offer("2", "player", "support", "Support"),
        offer("3", "other", "fire", "Fire")));

    ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
    verify(playerChannel).sendMessage(message.capture());
    assertTrue(message.getValue().contains("Fire, Support"));
    assertTrue(message.getValue().contains("Raid"));
    verify(otherChannel).sendMessage(anyString());
  }

  @Test
  void sendsOneCombinedNoticeToAnOrganizerWhoIsAlsoTheHost() {
    DiscordEventUIApplicationAdapter adapter = adapter();
    PrivateChannel hostChannel = prepareChannel(adapter, "host");
    adapter.announceBackup(event(), List.of(
        offer("1", "player", "fire", "Fire"),
        offer("2", "player", "support", "Support")));

    ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
    verify(hostChannel).sendMessage(message.capture());
    assertTrue(message.getValue().contains("Fire, Support"));
    assertTrue(message.getValue().contains("<@player>"));
  }

  private DiscordEventUIApplicationAdapter adapter() {
    DiscordEventUIApplicationAdapter adapter = new DiscordEventUIApplicationAdapter();
    adapter.api = mock(JDA.class);
    return adapter;
  }

  @SuppressWarnings("unchecked")
  private PrivateChannel prepareChannel(DiscordEventUIApplicationAdapter adapter, String userId) {
    User user = mock(User.class);
    CacheRestAction<User> retrieve = mock(CacheRestAction.class);
    when(adapter.api.retrieveUserById(userId)).thenReturn(retrieve);
    when(retrieve.complete()).thenReturn(user);
    CacheRestAction<PrivateChannel> open = mock(CacheRestAction.class);
    PrivateChannel channel = mock(PrivateChannel.class);
    when(user.openPrivateChannel()).thenReturn(open);
    when(open.complete()).thenReturn(channel);
    when(channel.sendMessage(anyString())).thenReturn(mock(MessageCreateAction.class, RETURNS_SELF));
    return channel;
  }

  private Event event() {
    GuildMember host = new GuildMember("host");
    return Event.builder().id(7L).hoster(host).eventAdmin(host)
        .template(new EventTemplate("raid", "Raid", "", "coin"))
        .start(Instant.now().plusSeconds(3600)).build();
  }

  private EventBackupOffer offer(String id, String user, String role, String name) {
    return new EventBackupOffer(id, 7L, user, role, name);
  }
}
