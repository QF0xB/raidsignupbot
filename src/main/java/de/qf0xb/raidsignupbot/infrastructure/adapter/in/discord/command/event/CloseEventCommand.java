package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.event;

import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.port.EventUIApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.EventRaidCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class CloseEventCommand extends EventRaidCommand {
  private static final String OPTION_NAME = "event";

  @Inject
  EventApplicationPort eventApplicationPort;

  @Inject
  EventUIApplicationPort eventUIApplicationPort;

  @Inject
  JDA api;

  public CloseEventCommand() {
    super("closeevent", "Closes the event and removes channels.", CommandAccessLevel.MANAGE_EVENTS);
    addOption(new OptionData(OptionType.STRING, OPTION_NAME, "The event to close", true, true));
  }

  @Override
  public EventApplicationPort getEventApplicationPort() {
    return eventApplicationPort;
  }

  @Override
  public JDA getApi() {
    return api;
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.deferReply().queue();

    String channelName = Objects.requireNonNull(event.getOption(OPTION_NAME),
        "Missing required option: " + OPTION_NAME).getAsString();
    Event raidEvent = getEventByChannelName(channelName);

    raidEvent.closeSignup();
    raidEvent.markFifteenMinutesBeforeEmitted();
    raidEvent.markOneHourAfterEmitted();
    raidEvent.markOneHourBeforeEmitted();
    raidEvent.markThirtyMinutesBeforeEmitted();
    eventUIApplicationPort.deleteEvent(raidEvent);
    eventApplicationPort.save(raidEvent);

    event.getHook().sendMessage("Event " + channelName + " successfully closed").queue();
  }

  @Override
  public void autoComplete(CommandAutoCompleteInteractionEvent event) {
    if (event.getFocusedOption().getName().equals(OPTION_NAME)) {
      List<String> channelIds = getIdsAndCache(eventApplicationPort.findAllChannelExistingEvents());

      List<Command.Choice> choices = getCachedChannelsByName().values().stream()
          .map(channel -> new Command.Choice(channel.getName(), channel.getName()))
          .toList();

      event.replyChoices(choices).queue();
    }
  }
}
