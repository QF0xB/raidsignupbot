package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.event;

import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.EventRaidCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class RemoveSignupCommand extends EventRaidCommand {
  private static final String OPTION_EVENT = "event";
  private static final String OPTION_MEMBER = "member";

  @Inject
  EventApplicationPort eventApplicationPort;

  @Inject
  EventService eventService;

  @Inject
  JDA api;

  public RemoveSignupCommand() {
    super("removesignup", "Removes a member from signup and regenerates the teams.", CommandAccessLevel.MANAGE_EVENTS);
    addOption(new OptionData(OptionType.STRING, OPTION_EVENT, "The event to update", true, true));
    addOption(new OptionData(OptionType.USER, OPTION_MEMBER, "The member to remove from the signup", true));
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

    try {
      String channelName = Objects.requireNonNull(event.getOption(OPTION_EVENT),
          "Missing required option: " + OPTION_EVENT).getAsString();
      User member = Objects.requireNonNull(event.getOption(OPTION_MEMBER),
          "Missing required option: " + OPTION_MEMBER).getAsUser();

      Event raidEvent = getEventByChannelName(channelName);
      eventService.removeSignupAndRegenerateTeams(raidEvent.getId(), member.getId());

      event.getHook()
          .sendMessage(String.format(
              "Removed %s from signup for event %s and regenerated the teams.",
              member.getAsMention(),
              channelName))
          .queue();
    } catch (Exception e) {
      event.getHook().sendMessage("Could not remove the selected member: " + e.getMessage()).queue();
    }
  }

  @Override
  public void autoComplete(CommandAutoCompleteInteractionEvent event) {
    if (event.getFocusedOption().getName().equals(OPTION_EVENT)) {
      getIdsAndCache(eventApplicationPort.findAllChannelExistingEvents());

      List<Command.Choice> choices = getCachedChannelsByName().values().stream()
          .map(channel -> new Command.Choice(channel.getName(), channel.getName()))
          .toList();

      event.replyChoices(choices).queue();
    }
  }
}
