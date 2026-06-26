package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command;

import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.Channel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class EventRaidCommand extends RaidCommand {
  @Getter
  private final Map<String, Channel> cachedChannelsByName = new HashMap<>();

  public EventRaidCommand(String commandName, String description) {
    super(commandName, description, CommandAccessLevel.MANAGE_EVENTS);
  }

  public EventRaidCommand(String commandName, String description, CommandAccessLevel accessLevel) {
    super(commandName, description, accessLevel);
  }

  public Event getEventByChannelName(String channelName) {
    Channel channel = cachedChannelsByName.get(channelName);
    return getEventApplicationPort().findByChannelId(channel.getId())
        .orElseThrow(() -> new IllegalArgumentException("Event with channel id " + channel.getId() + " not found"));
  }

  public List<String> getIdsAndCache(Set<Event> events) {
    List<String> channelIds = events
        .stream()
        .map(Event::getSignUpChannelId)
        .toList();

    channelIds.forEach(channelId -> {
      Channel channel = getApi().getTextChannelById(channelId);
      assert channel != null;
      cachedChannelsByName.put(channel.getName(), channel);
    });

    return channelIds;
  }

  public abstract EventApplicationPort getEventApplicationPort();
  public abstract JDA getApi();
}
