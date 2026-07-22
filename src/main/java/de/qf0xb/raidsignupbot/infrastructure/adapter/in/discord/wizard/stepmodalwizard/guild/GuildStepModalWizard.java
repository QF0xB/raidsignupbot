package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.guild;

import de.qf0xb.raidsignupbot.application.dto.CreateGuildCommand;
import de.qf0xb.raidsignupbot.application.dto.UpdateGuildCommand;
import de.qf0xb.raidsignupbot.application.service.guild.GuildService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizard;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizardSession;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizardStep;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.radiogroup.RadioGroup;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.entities.channel.ChannelType;
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
  private static final String GUILD_MANAGER_ROLE_ID_FIELD = "guild-manager-role-id";
  private static final String TEMPLATE_MANAGER_ROLE_ID_FIELD = "template-manager-role-id";
  private static final String EVENT_MANAGER_ROLE_ID_FIELD = "event-manager-role-id";
  private static final String MANAGE_SIGNUP_CHANNELS_FIELD = "manage-signup-channels";
  private static final String MANAGE_VOICE_CHANNELS_FIELD = "manage-voice-channels";
  private static final String RAID_VOICE_CHANNEL_CATEGORY_FIELD = "raid-voice-channel-category";

  @Inject
  GuildService guildService;

  @Inject
  ServerService serverService;

  public GuildStepModalWizard() {
    super("onboard", "Onboard a guild to the bot", CommandAccessLevel.MANAGE_GUILDS);
    addStep(new GeneralDetailsStep());
    addStep(new ServerRolesStep());
    addStep(new ChannelSettingsStep());
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

    serverService.findServerByIdOptional(existingGuild.getServerId()).ifPresent(server -> {
      if (server.getGuildManagerRoleId() != null) {
        session.putValue(GUILD_MANAGER_ROLE_ID_FIELD, server.getGuildManagerRoleId());
      }
      if (server.getTemplateManagerRoleId() != null) {
        session.putValue(TEMPLATE_MANAGER_ROLE_ID_FIELD, server.getTemplateManagerRoleId());
      }
      if (server.getEventManagerRoleId() != null) {
        session.putValue(EVENT_MANAGER_ROLE_ID_FIELD, server.getEventManagerRoleId());
      }
      session.putValue(MANAGE_SIGNUP_CHANNELS_FIELD, String.valueOf(server.isManageSignupChannels()));
      session.putValue(MANAGE_VOICE_CHANNELS_FIELD, String.valueOf(server.isManageVoiceChannels()));
      if (server.getRaidVoiceChannelCategory() != null) {
        session.putValue(RAID_VOICE_CHANNEL_CATEGORY_FIELD, server.getRaidVoiceChannelCategory());
      }
    });

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
    String raiderRoleId = raiderRolesEnabled ? optionalValue(session, RAIDER_ROLE_ID_FIELD) : null;
    String eventRoleId = optionalValue(session, EVENT_ROLE_ID_FIELD);
    String guildManagerRoleId = optionalValue(session, GUILD_MANAGER_ROLE_ID_FIELD);
    String templateManagerRoleId = optionalValue(session, TEMPLATE_MANAGER_ROLE_ID_FIELD);
    String eventManagerRoleId = optionalValue(session, EVENT_MANAGER_ROLE_ID_FIELD);
    boolean manageSignupChannels = parseBoolean(session.requireValue(MANAGE_SIGNUP_CHANNELS_FIELD), MANAGE_SIGNUP_CHANNELS_FIELD);
    boolean manageVoiceChannels = parseBoolean(session.requireValue(MANAGE_VOICE_CHANNELS_FIELD), MANAGE_VOICE_CHANNELS_FIELD);
    String raidVoiceChannelCategory = optionalValue(session, RAID_VOICE_CHANNEL_CATEGORY_FIELD);

    CreateGuildCommand createCommand = CreateGuildCommand.builder()
        .name(guildName)
        .serverId(serverId)
        .raiderRolesEnabled(raiderRolesEnabled)
        .raiderRoleId(raiderRoleId)
        .eventRoleId(eventRoleId)
        .build();

    Guild savedGuild;
    if (session.getValues().containsKey(ORIGINAL_GUILD_NAME_FIELD)) {
      UpdateGuildCommand updateCommand = UpdateGuildCommand.builder()
        .name(guildName)
        .serverId(serverId)
        .raiderRolesEnabled(raiderRolesEnabled)
        .raiderRoleId(raiderRoleId)
        .eventRoleId(eventRoleId)
        .build();

      savedGuild = guildService.updateGuild(session.requireValue(ORIGINAL_GUILD_NAME_FIELD), updateCommand);
    } else {
      savedGuild = guildService.createGuild(createCommand);
    }

    Server server = serverService.updateSettings(
        serverId,
        guildManagerRoleId,
        templateManagerRoleId,
        eventManagerRoleId,
        manageSignupChannels,
        manageVoiceChannels,
        raidVoiceChannelCategory);

    return (session.getValues().containsKey(ORIGINAL_GUILD_NAME_FIELD) ? "Guild updated: " : "Guild created: ")
        + savedGuild.getName()
        + "\n"
        + formatGuild(savedGuild)
        + "\n"
        + formatServer(server);
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
          ? "Edit guild details - step 1/3."
          : "Provide the guild details - step 1/3.";
    }

    @Override
    public Modal buildModal(String modalId, StepModalWizardSession session, ButtonInteractionEvent event) {
      String nameValue = session.getValues().get(NAME_FIELD);
      String raiderRolesEnabledValue = session.getValues().get(RAIDER_ROLES_ENABLED_FIELD);

      TextInput.Builder nameInput = TextInput.create(NAME_FIELD, TextInputStyle.SHORT)
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
          session.getValues().containsKey(ORIGINAL_GUILD_NAME_FIELD) ? "Edit Guild - 1/3" : "Onboard Guild - 1/3");

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

  private final class ServerRolesStep implements StepModalWizardStep {
    @Override
    public String prompt(StepModalWizardSession session) {
      return session.getValues().containsKey(ORIGINAL_GUILD_NAME_FIELD)
          ? "Edit server roles - step 2/3."
          : "Provide the server roles - step 2/3.";
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

      EntitySelectMenu.Builder guildManagerRoleSelect = EntitySelectMenu.create(GUILD_MANAGER_ROLE_ID_FIELD, EntitySelectMenu.SelectTarget.ROLE)
          .setRequired(false)
          .setMinValues(0)
          .setMaxValues(1)
          .setPlaceholder("Optional guild manager role");
      if (session.getValues().containsKey(GUILD_MANAGER_ROLE_ID_FIELD)) {
        guildManagerRoleSelect.setDefaultValues(EntitySelectMenu.DefaultValue.role(session.requireValue(GUILD_MANAGER_ROLE_ID_FIELD)));
      }

      EntitySelectMenu.Builder templateManagerRoleSelect = EntitySelectMenu.create(TEMPLATE_MANAGER_ROLE_ID_FIELD, EntitySelectMenu.SelectTarget.ROLE)
          .setRequired(false)
          .setMinValues(0)
          .setMaxValues(1)
          .setPlaceholder("Optional template manager role");
      if (session.getValues().containsKey(TEMPLATE_MANAGER_ROLE_ID_FIELD)) {
        templateManagerRoleSelect.setDefaultValues(EntitySelectMenu.DefaultValue.role(session.requireValue(TEMPLATE_MANAGER_ROLE_ID_FIELD)));
      }

      EntitySelectMenu.Builder eventManagerRoleSelect = EntitySelectMenu.create(EVENT_MANAGER_ROLE_ID_FIELD, EntitySelectMenu.SelectTarget.ROLE)
          .setRequired(false)
          .setMinValues(0)
          .setMaxValues(1)
          .setPlaceholder("Optional event manager role");
      if (session.getValues().containsKey(EVENT_MANAGER_ROLE_ID_FIELD)) {
        eventManagerRoleSelect.setDefaultValues(EntitySelectMenu.DefaultValue.role(session.requireValue(EVENT_MANAGER_ROLE_ID_FIELD)));
      }

      return Modal.create(modalId,
          session.getValues().containsKey(ORIGINAL_GUILD_NAME_FIELD) ? "Edit Guild - 2/3" : "Onboard Guild - 2/3")
          .addComponents(
              Label.of("Event role", eventRoleSelect.build()),
              Label.of("Guild manager role", guildManagerRoleSelect.build()),
              Label.of("Template manager role", templateManagerRoleSelect.build()),
              Label.of("Event manager role", eventManagerRoleSelect.build())
          )
          .build();
    }

    @Override
    public void submit(ModalInteractionEvent event, StepModalWizardSession session) {
      String eventRoleId = GuildStepModalWizard.this.optionalValue(event, EVENT_ROLE_ID_FIELD);
      if (eventRoleId != null) {
        session.putValue(EVENT_ROLE_ID_FIELD, eventRoleId);
      }

      String guildManagerRoleId = GuildStepModalWizard.this.optionalValue(event, GUILD_MANAGER_ROLE_ID_FIELD);
      if (guildManagerRoleId != null) {
        session.putValue(GUILD_MANAGER_ROLE_ID_FIELD, guildManagerRoleId);
      }

      String templateManagerRoleId = GuildStepModalWizard.this.optionalValue(event, TEMPLATE_MANAGER_ROLE_ID_FIELD);
      if (templateManagerRoleId != null) {
        session.putValue(TEMPLATE_MANAGER_ROLE_ID_FIELD, templateManagerRoleId);
      }

      String eventManagerRoleId = GuildStepModalWizard.this.optionalValue(event, EVENT_MANAGER_ROLE_ID_FIELD);
      if (eventManagerRoleId != null) {
        session.putValue(EVENT_MANAGER_ROLE_ID_FIELD, eventManagerRoleId);
      }
    }
  }

  private final class ChannelSettingsStep implements StepModalWizardStep {
    @Override
    public String prompt(StepModalWizardSession session) {
      return session.getValues().containsKey(ORIGINAL_GUILD_NAME_FIELD)
          ? "Edit channel management - step 3/3."
          : "Provide the channel management settings - step 3/3.";
    }

    @Override
    public Modal buildModal(String modalId, StepModalWizardSession session, ButtonInteractionEvent event) {
      String manageSignupChannelsValue = session.getValues().get(MANAGE_SIGNUP_CHANNELS_FIELD);
      String manageVoiceChannelsValue = session.getValues().get(MANAGE_VOICE_CHANNELS_FIELD);
      String raidVoiceCategoryValue = session.getValues().get(RAID_VOICE_CHANNEL_CATEGORY_FIELD);

      RadioGroup.Builder manageSignupChannelsInput = RadioGroup.create(MANAGE_SIGNUP_CHANNELS_FIELD)
          .addOption("Enabled", "true")
          .addOption("Disabled", "false")
          .setRequired(true);
      if (manageSignupChannelsValue != null) {
        manageSignupChannelsInput.setSelectedValue(manageSignupChannelsValue);
      }

      RadioGroup.Builder manageVoiceChannelsInput = RadioGroup.create(MANAGE_VOICE_CHANNELS_FIELD)
          .addOption("Enabled", "true")
          .addOption("Disabled", "false")
          .setRequired(true);
      if (manageVoiceChannelsValue != null) {
        manageVoiceChannelsInput.setSelectedValue(manageVoiceChannelsValue);
      }

      EntitySelectMenu.Builder raidVoiceChannelCategoryInput = EntitySelectMenu.create(RAID_VOICE_CHANNEL_CATEGORY_FIELD, EntitySelectMenu.SelectTarget.CHANNEL)
          .setChannelTypes(ChannelType.CATEGORY)
          .setRequired(false)
          .setMinValues(0)
          .setMaxValues(1)
          .setPlaceholder("Optional raid voice category");
      if (raidVoiceCategoryValue != null && !raidVoiceCategoryValue.isBlank()) {
        raidVoiceChannelCategoryInput.setDefaultValues(EntitySelectMenu.DefaultValue.channel(raidVoiceCategoryValue));
      }

      return Modal.create(modalId,
          session.getValues().containsKey(ORIGINAL_GUILD_NAME_FIELD) ? "Edit Guild - 3/3" : "Onboard Guild - 3/3")
          .addComponents(
              Label.of("Manage signup channels", manageSignupChannelsInput.build()),
              Label.of("Manage voice channels", manageVoiceChannelsInput.build()),
              Label.of("Raid voice category", raidVoiceChannelCategoryInput.build())
          )
          .build();
    }

    @Override
    public void submit(ModalInteractionEvent event, StepModalWizardSession session) {
      session.putValue(MANAGE_SIGNUP_CHANNELS_FIELD, requiredValue(event, MANAGE_SIGNUP_CHANNELS_FIELD));
      session.putValue(MANAGE_VOICE_CHANNELS_FIELD, requiredValue(event, MANAGE_VOICE_CHANNELS_FIELD));

      String raidVoiceCategory = optionalValue(event, RAID_VOICE_CHANNEL_CATEGORY_FIELD);
      if (raidVoiceCategory != null) {
        session.putValue(RAID_VOICE_CHANNEL_CATEGORY_FIELD, raidVoiceCategory);
      }
    }
  }

  private RadioGroup.Builder radioGroup(String id, String trueLabel, String trueValue, String falseLabel, String falseValue) {
    return RadioGroup.create(id)
        .addOption(trueLabel, trueValue)
        .addOption(falseLabel, falseValue)
        .setRequired(true);
  }

  private String formatGuild(Guild guild) {
    return "- Raider roles: " + yesNo(guild.isRaiderRolesEnabled()) + " (" + valueOrNone(guild.getRaiderRoleId()) + ")\n"
        + "- Event role: " + valueOrNone(guild.getEventRoleId());
  }

  private String formatServer(Server server) {
    String channelsMessage;
    if (server.isManageVoiceChannels() && server.isManageSignupChannels()) {
      channelsMessage = "voice + signup channels";
    } else if (server.isManageVoiceChannels()) {
      channelsMessage = "voice channels only";
    } else if (server.isManageSignupChannels()) {
      channelsMessage = "signup channels only";
    } else {
      channelsMessage = "none";
    }

    return "- Guild manager role: " + valueOrNone(server.getGuildManagerRoleId()) + "\n"
        + "- Template manager role: " + valueOrNone(server.getTemplateManagerRoleId()) + "\n"
        + "- Event manager role: " + valueOrNone(server.getEventManagerRoleId()) + "\n"
        + "- Managing channels: " + channelsMessage
        + (server.getRaidVoiceChannelCategory() == null ? "" : "\n- Raid voice category: " + server.getRaidVoiceChannelCategory());
  }

  private String valueOrNone(String value) {
    return value == null || value.isBlank() ? "none" : "<@&" + value + ">";
  }

  private String yesNo(boolean value) {
    return value ? "yes" : "no";
  }
}
