package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.guild;

import de.qf0xb.raidsignupbot.application.dto.CreateGuildCommand;
import de.qf0xb.raidsignupbot.application.dto.UpdateGuildCommand;
import de.qf0xb.raidsignupbot.application.service.guild.GuildService;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizard;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizardSession;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizardStep;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.radiogroup.RadioGroup;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;

import java.util.Objects;

@ApplicationScoped
public class GuildStepModalWizard extends StepModalWizard {
  private static final String ORIGINAL_GUILD_NAME_FIELD = "original-guild-name";
  private static final String SERVER_ID_FIELD = "server-id";
  private static final String NAME_FIELD = "guild-name";
  private static final String RAIDER_ROLES_ENABLED_FIELD = "raider-roles-enabled";
  private static final String RAIDER_ROLE_ID_FIELD = "raider-role-id";
  private static final String EVENT_ROLE_ID_FIELD = "event-role-id";

  @Inject
  GuildService guildService;

  public GuildStepModalWizard() {
    super("onboard", "Onboard a guild to the bot", CommandAccessLevel.MANAGE_GUILDS);
    addStep(new GeneralDetailsStep());
    addStep(new EventRoleStep());
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    cleanupExpiredSessions();

    if (event.getGuild() == null) {
      event.reply("This command can only be used in a guild.")
          .setEphemeral(true)
          .queue();
      return;
    }

    StepModalWizardSession session = createSession(event.getUser().getId());
    session.putValue(SERVER_ID_FIELD, event.getGuild().getId());
    registerSession(session);

    try {
      sendPrompt(event, session);
    } catch (RuntimeException ex) {
      sessions.remove(session.getSessionId());
      event.reply("Failed to start guild wizard: " + ex.getMessage())
          .setEphemeral(true)
          .queue();
    }
  }

  public void startEdit(SlashCommandInteractionEvent event, Guild existingGuild) {
    cleanupExpiredSessions();

    StepModalWizardSession session = createSession(event.getUser().getId());
    session.putValue(ORIGINAL_GUILD_NAME_FIELD, existingGuild.getName());
    session.putValue(SERVER_ID_FIELD, existingGuild.getServerId());
    session.putValue(NAME_FIELD, existingGuild.getName());
    session.putValue(RAIDER_ROLES_ENABLED_FIELD, String.valueOf(existingGuild.isRaiderRolesEnabled()));
    if (existingGuild.getRaiderRoleId() != null) {
      session.putValue(RAIDER_ROLE_ID_FIELD, existingGuild.getRaiderRoleId());
    }
    if (existingGuild.getEventRoleId() != null) {
      session.putValue(EVENT_ROLE_ID_FIELD, existingGuild.getEventRoleId());
    }

    registerSession(session);

    try {
      sendPrompt(event, session);
    } catch (RuntimeException ex) {
      sessions.remove(session.getSessionId());
      event.reply("Failed to start guild edit wizard: " + ex.getMessage())
          .setEphemeral(true)
          .queue();
    }
  }

  @Override
  public String onComplete(StepModalWizardSession session, ModalInteractionEvent event) {
    String guildName = session.requireValue(NAME_FIELD);
    String serverId = session.requireValue(SERVER_ID_FIELD);
    boolean raiderRolesEnabled = parseBoolean(session.requireValue(RAIDER_ROLES_ENABLED_FIELD), RAIDER_ROLES_ENABLED_FIELD);

    CreateGuildCommand createCommand = CreateGuildCommand.builder()
        .name(guildName)
        .serverId(serverId)
        .raiderRolesEnabled(raiderRolesEnabled)
        .raiderRoleId(optionalValue(session, RAIDER_ROLE_ID_FIELD))
        .eventRoleId(optionalValue(session, EVENT_ROLE_ID_FIELD))
        .build();

    if (session.getValues().containsKey(ORIGINAL_GUILD_NAME_FIELD)) {
      UpdateGuildCommand updateCommand = UpdateGuildCommand.builder()
          .name(guildName)
          .serverId(serverId)
          .raiderRolesEnabled(raiderRolesEnabled)
          .raiderRoleId(optionalValue(session, RAIDER_ROLE_ID_FIELD))
          .eventRoleId(optionalValue(session, EVENT_ROLE_ID_FIELD))
          .build();

      Guild updated = guildService.updateGuild(session.requireValue(ORIGINAL_GUILD_NAME_FIELD), updateCommand);
      return "Guild updated: " + updated.getName();
    }

    Guild created = guildService.createGuild(createCommand);
    return "Guild created: " + created.getName();
  }

  @Override
  public boolean supportsStringSelectId(String stringSelectId) {
    return false;
  }

  @Override
  public void onStringSelectInteraction(StringSelectInteractionEvent event) {
    throw new UnsupportedOperationException("StringSelectInteractionEvent not supported");
  }

  @Override
  public boolean supportsEntitySelectId(String entitySelectId) {
    return false;
  }

  @Override
  public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
    throw new UnsupportedOperationException("EntitySelectInteractionEvent not supported");
  }

  private boolean parseBoolean(String value, String fieldName) {
    String normalized = value == null ? "" : value.trim().toLowerCase();
    return switch (normalized) {
      case "true", "yes", "on", "1" -> true;
      case "false", "no", "off", "0" -> false;
      default -> throw new IllegalArgumentException("Invalid boolean value for " + fieldName + ": " + value);
    };
  }

  private String optionalValue(StepModalWizardSession session, String fieldName) {
    String value = session.getValues().get(fieldName);
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private String optionalValue(ModalInteractionEvent event, String fieldName) {
    if (event.getValue(fieldName) == null) {
      return null;
    }
    String value = requiredValue(event, fieldName);
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private final class GeneralDetailsStep implements StepModalWizardStep {
    @Override
    public String prompt(StepModalWizardSession session) {
      return session.getValues().containsKey(ORIGINAL_GUILD_NAME_FIELD)
          ? "Edit guild details - step 1/2."
          : "Provide the guild details - step 1/2.";
    }

    @Override
    public Modal buildModal(String modalId, StepModalWizardSession session, ButtonInteractionEvent event) {
      String nameValue = session.getValues().get(NAME_FIELD);
      String raiderRolesEnabledValue = session.getValues().get(RAIDER_ROLES_ENABLED_FIELD);

      var nameInput = net.dv8tion.jda.api.components.textinput.TextInput.create(NAME_FIELD, net.dv8tion.jda.api.components.textinput.TextInputStyle.SHORT)
          .setRequired(true)
          .setMaxLength(255);
      if (nameValue != null && !nameValue.isBlank()) {
        nameInput.setValue(nameValue);
      }

      RadioGroup.Builder raiderRolesEnabledInput = RadioGroup.create(RAIDER_ROLES_ENABLED_FIELD)
          .addOption("Enabled", "true")
          .addOption("Disabled", "false")
          .setRequired(true);
      if (raiderRolesEnabledValue != null) {
        raiderRolesEnabledInput.setSelectedValue(raiderRolesEnabledValue);
      }

      EntitySelectMenu.Builder raiderRoleSelect = EntitySelectMenu.create(RAIDER_ROLE_ID_FIELD, EntitySelectMenu.SelectTarget.ROLE)
          .setRequired(false)
          .setMinValues(0)
          .setMaxValues(1)
          .setPlaceholder("Optional raider role");
      if (session.getValues().containsKey(RAIDER_ROLE_ID_FIELD)) {
        raiderRoleSelect.setDefaultValues(EntitySelectMenu.DefaultValue.role(session.requireValue(RAIDER_ROLE_ID_FIELD)));
      }

      Modal.Builder builder = Modal.create(modalId,
          session.getValues().containsKey(ORIGINAL_GUILD_NAME_FIELD) ? "Edit Guild - 1/2" : "Onboard Guild - 1/2");

      builder.addComponents(
          Label.of("Guild name", nameInput.build()),
          Label.of("Raider roles enabled", raiderRolesEnabledInput.build()),
          Label.of("Raider role", raiderRoleSelect.build())
      );

      return builder.build();
    }

    @Override
    public void submit(ModalInteractionEvent event, StepModalWizardSession session) {
      session.putValue(NAME_FIELD, requiredValue(event, NAME_FIELD));
      session.putValue(RAIDER_ROLES_ENABLED_FIELD, requiredValue(event, RAIDER_ROLES_ENABLED_FIELD));

      String raiderRoleId = GuildStepModalWizard.this.optionalValue(event, RAIDER_ROLE_ID_FIELD);
      if (raiderRoleId != null) {
        session.putValue(RAIDER_ROLE_ID_FIELD, raiderRoleId);
      }
    }
  }

  private final class EventRoleStep implements StepModalWizardStep {
    @Override
    public String prompt(StepModalWizardSession session) {
      return session.getValues().containsKey(ORIGINAL_GUILD_NAME_FIELD)
          ? "Edit guild details - step 2/2."
          : "Provide the guild details - step 2/2.";
    }

    @Override
    public Modal buildModal(String modalId, StepModalWizardSession session, ButtonInteractionEvent event) {
      EntitySelectMenu.Builder eventRoleSelect = EntitySelectMenu.create(EVENT_ROLE_ID_FIELD, EntitySelectMenu.SelectTarget.ROLE)
          .setRequired(false)
          .setMinValues(0)
          .setMaxValues(1)
          .setPlaceholder("Optional event role");
      if (session.getValues().containsKey(EVENT_ROLE_ID_FIELD)) {
        eventRoleSelect.setDefaultValues(EntitySelectMenu.DefaultValue.role(session.requireValue(EVENT_ROLE_ID_FIELD)));
      }

      return Modal.create(modalId,
          session.getValues().containsKey(ORIGINAL_GUILD_NAME_FIELD) ? "Edit Guild - 2/2" : "Onboard Guild - 2/2")
          .addComponents(
              Label.of("Event role", eventRoleSelect.build())
          )
          .build();
    }

    @Override
    public void submit(ModalInteractionEvent event, StepModalWizardSession session) {
      String eventRoleId = GuildStepModalWizard.this.optionalValue(event, EVENT_ROLE_ID_FIELD);
      if (eventRoleId != null) {
        session.putValue(EVENT_ROLE_ID_FIELD, eventRoleId);
      }
    }
  }
}
