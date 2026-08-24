package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.calendar;

import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.calendar.RaidCalendarPublisher;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizard;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizardSession;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizardStep;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;

import java.util.Objects;

@ApplicationScoped
public class CalendarConfigCommand extends StepModalWizard {
  private static final String CHANNEL_FIELD = "calendar-channel";
  private static final String OWNER_FIELD = "calendar-owner";

  @Inject
  ServerService serverService;

  @Inject
  RaidCalendarPublisher publisher;

  public CalendarConfigCommand() {
    super("calendarconfig", "Configure the raid calendar channel and owner.", CommandAccessLevel.MANAGE_GUILDS);
    addStep(new SettingsStep());
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
    String serverId = Objects.requireNonNull(event.getGuild(), "This command can only be used in a guild.").getId();
    String channelId = session.requireValue(CHANNEL_FIELD);
    String ownerId = session.requireValue(OWNER_FIELD);
    Server server = serverService.updateCalendarSettings(serverId, channelId, ownerId);
    publisher.publish(serverId);
    return "Raid calendar configured: <#" + server.getRaidCalendarChannelId()
        + ">, owner <@" + server.getRaidCalendarOwnerId() + ">.";
  }

  private final class SettingsStep implements StepModalWizardStep {
    @Override
    public String prompt(StepModalWizardSession session) {
      return "Configure the raid calendar channel and the only member allowed to create events.";
    }

    @Override
    public Modal buildModal(String modalId, StepModalWizardSession session, ButtonInteractionEvent event) {
      EntitySelectMenu channelSelect = EntitySelectMenu.create(CHANNEL_FIELD, EntitySelectMenu.SelectTarget.CHANNEL)
          .setChannelTypes(ChannelType.TEXT)
          .setRequired(true)
          .build();
      EntitySelectMenu ownerSelect = EntitySelectMenu.create(OWNER_FIELD, EntitySelectMenu.SelectTarget.USER)
          .setRequired(true)
          .build();

      return Modal.create(modalId, "Raid Calendar Configuration")
          .addComponents(
              Label.of("Calendar channel:", channelSelect),
              Label.of("Event owner:", ownerSelect))
          .build();
    }

    @Override
    public void submit(ModalInteractionEvent event, StepModalWizardSession session) {
      session.putValue(CHANNEL_FIELD, requiredValue(event, CHANNEL_FIELD));
      session.putValue(OWNER_FIELD, requiredValue(event, OWNER_FIELD));
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
