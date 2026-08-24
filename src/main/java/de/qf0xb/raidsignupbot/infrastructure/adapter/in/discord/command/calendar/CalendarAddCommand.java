package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.calendar;

import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.calendar.RaidCalendarPublisher;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizard;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizardSession;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizardStep;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.radiogroup.RadioGroup;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class CalendarAddCommand extends StepModalWizard {
  private static final String GUILD_FIELD = "calendar-guild";
  private static final String TEMPLATE_FIELD = "calendar-template";
  private static final String HOSTER_FIELD = "calendar-hoster";
  private static final String START_FIELD = "calendar-start";
  private static final String DURATION_FIELD = "calendar-duration";

  @Inject
  RaidCalendarService calendarService;

  @Inject
  RaidCalendarPublisher publisher;

  @Inject
  GuildApplicationPort guildApplicationPort;

  @Inject
  EventTemplateApplicationPort templateApplicationPort;

  public CalendarAddCommand() {
    super("scheduleevent", "Schedule an event on the raid calendar.", CommandAccessLevel.MANAGE_GUILDS);
    addStep(new AddEntryStep());
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
      Instant start = Instant.ofEpochSecond(Long.parseLong(session.requireValue(START_FIELD).trim()));
      Duration duration = parseDuration(session.requireValue(DURATION_FIELD));
      var entry = calendarService.createEntry(
          serverId,
          session.requireValue(GUILD_FIELD),
          session.requireValue(HOSTER_FIELD),
          session.requireValue(TEMPLATE_FIELD),
          start,
          start.plus(duration));
      publisher.publish(serverId);
      return "Added calendar entry " + entry.getId() + ".";
    } catch (RuntimeException ex) {
      return "Could not add calendar entry: " + ex.getMessage();
    }
  }

  private final class AddEntryStep implements StepModalWizardStep {
    @Override
    public String prompt(StepModalWizardSession session) {
      return "Add a scheduled event to the raid calendar.";
    }

    @Override
    public Modal buildModal(String modalId, StepModalWizardSession session, ButtonInteractionEvent event) {
      String serverId = Objects.requireNonNull(event.getGuild(), "This command can only be used in a guild.").getId();
      Set<SelectOption> guildOptions = guildApplicationPort.findGuildByServerId(serverId).stream()
          .limit(25)
          .map(guild -> SelectOption.of(guild.getName(), guild.getName()))
          .collect(Collectors.toSet());
      Set<SelectOption> templateOptions = templateApplicationPort.findAllByServerId(serverId).stream()
          .limit(25)
          .map(template -> SelectOption.of(template.getKey(), template.getKey()).withDescription(template.getName()))
          .collect(Collectors.toSet());

      StringSelectMenu guildSelect = StringSelectMenu.create(GUILD_FIELD)
          .addOptions(guildOptions)
          .setRequired(true)
          .build();
      StringSelectMenu templateSelect = StringSelectMenu.create(TEMPLATE_FIELD)
          .addOptions(templateOptions)
          .setRequired(true)
          .build();
      EntitySelectMenu hosterSelect = EntitySelectMenu.create(HOSTER_FIELD, EntitySelectMenu.SelectTarget.USER)
          .setRequired(true)
          .build();
      TextInput startInput = TextInput.create(START_FIELD, TextInputStyle.SHORT)
          .setPlaceholder(String.valueOf(Instant.now().getEpochSecond()))
          .setRequired(true)
          .build();
      RadioGroup durationSelect = RadioGroup.create(DURATION_FIELD)
          .addOption("1 hour", "1h")
          .addOption("1.5 hours", "1,5h")
          .addOption("2 hours", "2h")
          .addOption("3 hours", "3h")
          .setRequired(true)
          .build();

      return Modal.create(modalId, "Add Raid Calendar Entry")
          .addComponents(
              Label.of("Guild:", guildSelect),
              Label.of("Event template:", templateSelect),
              Label.of("Host:", hosterSelect),
              Label.of("Start Unix timestamp:", startInput),
              Label.of("Duration:", durationSelect))
          .build();
    }

    @Override
    public void submit(ModalInteractionEvent event, StepModalWizardSession session) {
      session.putValue(GUILD_FIELD, requiredValue(event, GUILD_FIELD));
      session.putValue(TEMPLATE_FIELD, requiredValue(event, TEMPLATE_FIELD));
      session.putValue(HOSTER_FIELD, requiredValue(event, HOSTER_FIELD));
      String start = requiredValue(event, START_FIELD);
      try {
        Instant.ofEpochSecond(Long.parseLong(start.trim()));
      } catch (NumberFormatException ex) {
        throw new IllegalArgumentException("Start must be a Unix epoch timestamp");
      }
      session.putValue(START_FIELD, start);
      session.putValue(DURATION_FIELD, requiredValue(event, DURATION_FIELD));
    }
  }

  private Duration parseDuration(String value) {
    return switch (value) {
      case "1h" -> Duration.ofHours(1);
      case "1,5h" -> Duration.ofMinutes(90);
      case "2h" -> Duration.ofHours(2);
      case "3h" -> Duration.ofHours(3);
      default -> throw new IllegalArgumentException("Unsupported duration");
    };
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
