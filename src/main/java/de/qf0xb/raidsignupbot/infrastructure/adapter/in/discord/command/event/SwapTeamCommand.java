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
public class SwapTeamCommand extends EventRaidCommand {
  private static final String OPTION_EVENT = "event";
  private static final String OPTION_LEFT_MEMBER = "left_member";
  private static final String OPTION_RIGHT_MEMBER = "right_member";

  @Inject
  EventApplicationPort eventApplicationPort;

  @Inject
  EventService eventService;

  @Inject
  JDA api;

  public SwapTeamCommand() {
    super("swapteam", "Swaps one primary slot per player, or substitutes an unassigned player.", CommandAccessLevel.MANAGE_EVENTS);
    addOption(new OptionData(OptionType.STRING, OPTION_EVENT, "The event to update", true, true));
    addOption(new OptionData(OptionType.USER, OPTION_LEFT_MEMBER, "First team member", true));
    addOption(new OptionData(OptionType.USER, OPTION_RIGHT_MEMBER, "Second team member", true));
    addOption(new OptionData(OptionType.STRING, "left_slot", "First player's slot ID from the teams message, e.g. 1:2", false));
    addOption(new OptionData(OptionType.STRING, "right_slot", "Second player's slot ID from the teams message, e.g. 2:1", false));
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
      User leftMember = Objects.requireNonNull(event.getOption(OPTION_LEFT_MEMBER),
          "Missing required option: " + OPTION_LEFT_MEMBER).getAsUser();
      User rightMember = Objects.requireNonNull(event.getOption(OPTION_RIGHT_MEMBER),
          "Missing required option: " + OPTION_RIGHT_MEMBER).getAsUser();

      Event raidEvent = getEventByChannelName(channelName);
      if (event.getGuild() == null || raidEvent.getGuild() == null
          || !event.getGuild().getId().equals(raidEvent.getGuild().getServerId())) {
        throw new IllegalArgumentException("Select a raid from this server.");
      }
      String leftSlot = event.getOption("left_slot") == null ? null : event.getOption("left_slot").getAsString();
      String rightSlot = event.getOption("right_slot") == null ? null : event.getOption("right_slot").getAsString();
      eventService.swapTeamMembers(raidEvent.getId(), leftMember.getId(), rightMember.getId(), leftSlot, rightSlot);

      event.getHook()
          .sendMessage(String.format(
              "Updated one primary slot each for %s and %s in event %s. Other boxes are unchanged.",
              leftMember.getAsMention(),
              rightMember.getAsMention(),
              channelName))
          .queue();
    } catch (Exception e) {
      event.getHook().sendMessage("Could not swap the selected members: " + e.getMessage()).queue();
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
