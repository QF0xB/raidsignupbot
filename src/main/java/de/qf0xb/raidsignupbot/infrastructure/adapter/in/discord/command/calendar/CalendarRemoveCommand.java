package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.calendar;

import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.calendar.RaidCalendarPublisher;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizard;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizardSession;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizardStep;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;

import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class CalendarRemoveCommand extends StepModalWizard {
  private static final String ENTRY_FIELD = "calendar-entry";

  @Inject
  RaidCalendarService calendarService;

  @Inject
  RaidCalendarPublisher publisher;

  public CalendarRemoveCommand() {
    super("calendarremove", "Remove an event from the raid calendar.", CommandAccessLevel.MANAGE_GUILDS);
    addStep(new RemoveEntryStep());
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    if (event.getGuild() == null) {
      event.reply("This command can only be used in a guild.").setEphemeral(true).queue();
      return;
    }
    super.execute(event);
  }

  @Override
  public String onComplete(StepModalWizardSession session, ModalInteractionEvent event) {
    try {
      String serverId = Objects.requireNonNull(event.getGuild(), "This command can only be used in a guild.").getId();
      Long entryId = Long.parseLong(session.requireValue(ENTRY_FIELD));
      calendarService.deleteEntry(serverId, entryId);
      publisher.publish(serverId);
      return "Removed calendar entry " + entryId + ".";
    } catch (RuntimeException ex) {
      return "Could not remove calendar entry: " + ex.getMessage();
    }
  }

  private final class RemoveEntryStep implements StepModalWizardStep {
    @Override
    public String prompt(StepModalWizardSession session) {
      return "Choose an unused event to remove from the raid calendar.";
    }

    @Override
    public Modal buildModal(String modalId, StepModalWizardSession session, ButtonInteractionEvent event) {
      String serverId = Objects.requireNonNull(event.getGuild(), "This command can only be used in a guild.").getId();
      var options = calendarService.findEntries(serverId).stream()
          .filter(entry -> entry.getEventId() == null)
          .limit(25)
          .map(entry -> SelectOption.of(
              entry.getId() + " - " + entry.getGuildName() + " / " + entry.getTemplateKey(),
              String.valueOf(entry.getId())))
          .collect(Collectors.toSet());
      if (options.isEmpty()) {
        throw new IllegalStateException("There are no removable calendar entries");
      }

      StringSelectMenu entrySelect = StringSelectMenu.create(ENTRY_FIELD)
          .addOptions(options)
          .setRequired(true)
          .build();
      return Modal.create(modalId, "Remove Raid Calendar Entry")
          .addComponents(Label.of("Calendar entry:", entrySelect))
          .build();
    }

    @Override
    public void submit(ModalInteractionEvent event, StepModalWizardSession session) {
      session.putValue(ENTRY_FIELD, requiredValue(event, ENTRY_FIELD));
    }
  }

  @Override
  public boolean supportsStringSelectId(String stringSelectId) {
    return false;
  }

  @Override
  public void onStringSelectInteraction(StringSelectInteractionEvent event) {
  }

  @Override
  public boolean supportsEntitySelectId(String entitySelectId) {
    return false;
  }

  @Override
  public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
  }
}
