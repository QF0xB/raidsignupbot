package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.calendar;

import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

@ApplicationScoped
public class RaidCalendarPublisher {
  @Inject
  JDA api;

  @Inject
  RaidCalendarService calendarService;

  @Inject
  ServerService serverService;

  public void publish(String serverId) {
    Server server = calendarService.requireCalendarConfigured(serverId);
    TextChannel channel = api.getTextChannelById(server.getRaidCalendarChannelId());
    if (channel == null) {
      throw new IllegalStateException("Configured raid calendar channel does not exist");
    }

    String content = calendarService.render(serverId);
    if (server.getRaidCalendarMessageId() != null) {
      try {
        channel.retrieveMessageById(server.getRaidCalendarMessageId())
            .complete()
            .editMessage(content)
            .complete();
        return;
      } catch (RuntimeException ignored) {
        // The message may have been deleted. Publish a replacement below.
      }
    }

    Message message = channel.sendMessage(content).complete();
    server.setRaidCalendarMessageId(message.getId());
    serverService.save(server);
  }
}
