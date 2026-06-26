package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.event;

import de.qf0xb.raidsignupbot.AppSetup;
import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.EventRaidCommand;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.RaidCommand;
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
public class CloseSignupCommand extends EventRaidCommand {
  private static final String OPTION_NAME = "event";

  @Inject
  JDA api;

  @Inject
  EventService eventService;

  @Inject
  EventApplicationPort eventApplicationPort;

  @Inject
  AppSetup appSetup;

  public CloseSignupCommand() {
    super("closesignup", "Closes the signup for an event in a specific channel.", CommandAccessLevel.MANAGE_EVENTS);
    addOption(new OptionData(
        OptionType.STRING, OPTION_NAME,
        "The event to close signup and generate/post teams",
        true,
        true
    ));
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
    eventService.generateTeamsAndCloseSignup(raidEvent.getId(),
        /* for testing! use null after*/ appSetup.getSignups());

    event.getHook().sendMessage("Signup for event " + channelName + " closed.").queue();
  }

  @Override
  public void autoComplete(CommandAutoCompleteInteractionEvent event) {
    if (event.getFocusedOption().getName().equals(OPTION_NAME)) {
      List<String> channelIds = getIdsAndCache(eventApplicationPort.findAllUpcomingEvents());

      List<Command.Choice> choices = getCachedChannelsByName().values().stream()
          .map(channel -> new Command.Choice(channel.getName(), channel.getName()))
          .toList();

      event.replyChoices(choices).queue();
    }
  }
}
