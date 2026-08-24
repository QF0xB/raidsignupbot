package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.event;

import de.qf0xb.raidsignupbot.application.dto.CreateEventCommand;
import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizard;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizardSession;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizardStep;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.radiogroup.RadioGroup;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class EventCreationStepModalWizard extends StepModalWizard {
  // Static information
  private static final String TEMPLATE_FIELD = "template-key";
  private static final String HOSTER_FIELD = "hoster-user-id";
  private static final String EVENT_ADMIN_FIELD = "event-admin-user-id";
  private static final String GUILD_FIELD = "guild-id";
  private static final String START_FIELD = "start-timestamp";
  private static final String DURATION_FIELD = "duration";
  private static final String TEAMS_APPROVAL_FIELD = "teams-approval";
  private static final String CALENDAR_ENTRY_FIELD = "calendar-entry-id";

  // Donation
  private static final String DONATION_AMOUNT_FIELD = "donation-amount";
  private static final String DONATION_TYPE_FIELD = "donation-type";

  // Channels
  private static final String CATEGORY_FIELD = "category-id";
  private static final String SIGNUP_FIELD = "signup-channel-id";
  private static final String VOICE_FIELD = "voice-channel-id";
  private static final String EVENT_ID_FIELD = "event-id";

  @Inject
  JDA api;

  @Inject
  EventTemplateApplicationPort templateApplicationPort;

  @Inject
  GuildApplicationPort guildApplicationPort;
  @Inject
  ServerService serverService;
  @Inject
  EventService eventService;

  @Inject
  RaidCalendarService raidCalendarService;

  public EventCreationStepModalWizard() {
    super("createevent", "Create an event from the raid calendar", CommandAccessLevel.PUBLIC);
    addStep(new DetailsStep());
    addStep(new DonationAndChannelStep());
    addStep(new ApprovalStep());
  }

  @Override
  public String onComplete(StepModalWizardSession session, ModalInteractionEvent event) {
    Duration duration = switch (session.requireValue(DURATION_FIELD)) {
      case "1h" -> Duration.ofHours(1);
      case "1,5h" -> Duration.ofMinutes(90);
      case "2h" -> Duration.ofHours(2);
      case "3h" -> Duration.ofHours(3);
      default -> throw new IllegalArgumentException("Invalid duration");
    };

    String guildName = session.requireValue(GUILD_FIELD);
    String serverId = Objects.requireNonNull(event.getGuild(), "This command is only runnable on a guild.").getId();
    Guild guild = guildApplicationPort.findByServerIdAndName(serverId, guildName)
        .orElseThrow(() -> new IllegalArgumentException("Guild " + guildName + " not found"));
    Server server = serverService.findServerById(serverId);


    CreateEventCommand.CreateEventCommandBuilder commandBuilder = CreateEventCommand.builder()
        .serverId(serverId)
        .creatorUserId(session.getUserId())
        .guildName(guild.getName())
        .hosterUserId(session.requireValue(HOSTER_FIELD))
        .templateKey(session.requireValue(TEMPLATE_FIELD))
        .start(parseTimestamp(session.requireValue(START_FIELD)))
        .end(parseTimestamp(session.requireValue(START_FIELD)).plus(duration))
        .donation(parseDonation(session.requireValue(DONATION_AMOUNT_FIELD)))
        .donationPerWin(session.requireValue(DONATION_TYPE_FIELD).equals("per-win"))
        .eventAdminUserId(session.requireValue(EVENT_ADMIN_FIELD))
        .teamsRequireApproval(Boolean.parseBoolean(session.requireValue(TEAMS_APPROVAL_FIELD)));

    if (!session.getValues().containsKey(EVENT_ID_FIELD)) {
      commandBuilder.calendarEntryId(Long.parseLong(session.requireValue(CALENDAR_ENTRY_FIELD)));
    }

    if (requiresCategory(server)) {
      commandBuilder.categoryId(session.requireValue(CATEGORY_FIELD));
    }
    if (!server.isManageSignupChannels()) {
      commandBuilder.signUpChannelId(session.requireValue(SIGNUP_FIELD));
    }
    if (!server.isManageVoiceChannels()) {
      commandBuilder.voiceChannelId(session.requireValue(VOICE_FIELD));
    }

    Event savedEvent;
    if (session.getValues().containsKey(EVENT_ID_FIELD)) {
      savedEvent = eventService.updateEvent(Long.parseLong(session.requireValue(EVENT_ID_FIELD)), commandBuilder.build());
    } else {
      savedEvent = eventService.createEvent(commandBuilder.build());
    }

    String channelMention = savedEvent.getSignUpChannelId() == null
        ? "the event channel"
        : "<#" + savedEvent.getSignUpChannelId() + ">";
    return session.getValues().containsKey(EVENT_ID_FIELD)
        ? "Event updated: " + savedEvent.getId() + " in " + channelMention
        : "Event created: " + savedEvent.getId() + " in " + channelMention;
  }

  public void startEdit(SlashCommandInteractionEvent event, Event existingEvent) {
    cleanupExpiredSessions();

    StepModalWizardSession session = createSession(event.getUser().getId());
    session.putValue(EVENT_ID_FIELD, String.valueOf(existingEvent.getId()));
    session.putValue(TEMPLATE_FIELD, existingEvent.getTemplate().getKey());
    session.putValue(HOSTER_FIELD, existingEvent.getHoster().getUserId());
    session.putValue(EVENT_ADMIN_FIELD, existingEvent.getEventAdmin().getUserId());
    session.putValue(GUILD_FIELD, existingEvent.getGuild().getName());
    session.putValue(START_FIELD, String.valueOf(existingEvent.getStart().getEpochSecond()));
    session.putValue(DURATION_FIELD, existingEvent.getDuration());
    session.putValue(DONATION_AMOUNT_FIELD, String.valueOf(existingEvent.getDonation()));
    session.putValue(DONATION_TYPE_FIELD, existingEvent.isDonationPerWin() ? "per-win" : "one-time");
    session.putValue(TEAMS_APPROVAL_FIELD, String.valueOf(existingEvent.isTeamsRequireApproval()));
    Server server = serverService.findServerById(existingEvent.getGuild().getServerId());
    if (requiresCategory(server) && existingEvent.getCategoryId() != null) {
      session.putValue(CATEGORY_FIELD, existingEvent.getCategoryId());
    }
    if (!server.isManageSignupChannels() && existingEvent.getSignUpChannelId() != null) {
      session.putValue(SIGNUP_FIELD, existingEvent.getSignUpChannelId());
    }
    if (!server.isManageVoiceChannels() && existingEvent.getVoiceChannelId() != null) {
      session.putValue(VOICE_FIELD, existingEvent.getVoiceChannelId());
    }

    registerSession(session);

    try {
      sendPrompt(event, session);
    } catch (RuntimeException ex) {
      sessions.remove(session.getSessionId());
      event.reply("Failed to start edit wizard: " + ex.getMessage())
          .setEphemeral(true)
          .queue();
    }
  }

  public Instant parseTimestamp(String timestamp) {
    try {
      return Instant.ofEpochSecond(Long.parseLong(timestamp));
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Timestamp must be an epoch second");
    }
  }

  public int parseDonation(String value) {
    try {
      int donation = Integer.parseInt(value.trim());
      if (donation < 0) {
        throw new IllegalArgumentException("Donation must not be negative");
      }
      return donation;
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Donation must be a whole number");
    }
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

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    if (event.getGuild() == null) {
      event.reply("This command can only be used in a guild.")
          .setEphemeral(true)
          .queue();
      return;
    }

    try {
      Server server = raidCalendarService.requireCalendarConfigured(event.getGuild().getId());
      if (!event.getUser().getId().equals(server.getRaidCalendarOwnerId())) {
        event.reply("Only the configured raid calendar owner can create events.")
            .setEphemeral(true)
            .queue();
        return;
      }
      if (raidCalendarService.findUpcomingEntries(event.getGuild().getId()).isEmpty()) {
        event.reply("There are no available events on the raid calendar.")
            .setEphemeral(true)
            .queue();
        return;
      }
    } catch (RuntimeException ex) {
      event.reply(ex.getMessage() == null ? "The raid calendar is not ready." : ex.getMessage())
          .setEphemeral(true)
          .queue();
      return;
    }
    super.execute(event);
  }

  private final class DetailsStep implements StepModalWizardStep {
    @Override
    public String prompt(StepModalWizardSession session) {
      return "Step 1/3: provide general details of the event. " +
          "Please use https://www.unixtimestamp.com/ for the timestamp";
    }

    @Override
    public Modal buildModal(String modalId, StepModalWizardSession session, ButtonInteractionEvent event) {
      if (!session.getValues().containsKey(EVENT_ID_FIELD)) {
        Set<SelectOption> calendarOptions = raidCalendarService.findUpcomingEntries(
                Objects.requireNonNull(event.getGuild(), "This command is only runnable on a guild.").getId())
            .stream()
            .limit(25)
            .map(entry -> SelectOption.of(
                entry.getId() + " - " + entry.getGuildName() + " / " + entry.getTemplateKey(),
                String.valueOf(entry.getId())))
            .collect(Collectors.toSet());

        StringSelectMenu calendarSelect = StringSelectMenu.create(CALENDAR_ENTRY_FIELD)
            .addOptions(calendarOptions)
            .setRequired(true)
            .build();
        return Modal.create(modalId, "Create Event - Choose Calendar Entry")
            .addComponents(Label.of("Calendar entry:", calendarSelect))
            .build();
      }

      Server server = serverService.findServerById(
          Objects.requireNonNull(event.getGuild(), "This command is only runnable on a guild.").getId());

      Set<SelectOption> templateOptions = templateApplicationPort.findAllByServerId(Objects.requireNonNull(event.getGuild(), "This command is only runnable on a guild.").getId()).stream()
          .map(template -> SelectOption.of(template.getKey(), template.getKey()).withDescription(template.getName()))
          .collect(Collectors.toSet());

      StringSelectMenu templateSelect = StringSelectMenu.create(TEMPLATE_FIELD)
          .addOptions(templateOptions)
          .setRequired(true)
          .build();

      EntitySelectMenu hosterSelect = EntitySelectMenu.create(HOSTER_FIELD, EntitySelectMenu.SelectTarget.USER)
          .setRequired(true)
          .setDefaultValues(EntitySelectMenu.DefaultValue.from(api.retrieveUserById(
              session.getValues().getOrDefault(HOSTER_FIELD, event.getUser().getId())).complete()))
          .build();

      Set<SelectOption> guildOptions = guildApplicationPort.findGuildByServerId(Objects.requireNonNull(event.getGuild(), "This command is only runnable on a guild.").getId()).stream()
          .map(guild -> SelectOption.of(guild.getName(), guild.getName()))
          .collect(Collectors.toSet());

      StringSelectMenu guildSelect = StringSelectMenu.create(GUILD_FIELD)
          .addOptions(guildOptions)
          .build();

      TextInput timestampInput = TextInput.create(START_FIELD, TextInputStyle.SHORT)
          .setPlaceholder(String.valueOf(Instant.now().getEpochSecond()))
          .setRequired(true)
          .build();

      Set<SelectOption> durationOptions = Set.of(
          SelectOption.of("1h", "1h"),
          SelectOption.of("1,5h", "1,5h"),
          SelectOption.of("2h", "2h"),
          SelectOption.of("3h", "3h")
      );

      StringSelectMenu durationSelect = StringSelectMenu.create(DURATION_FIELD)
          .addOptions(durationOptions)
          .setRequired(true)
          .build();

      if (session.getValues().containsKey(START_FIELD)) {
        timestampInput = TextInput.create(START_FIELD, TextInputStyle.SHORT)
            .setValue(session.requireValue(START_FIELD))
            .setRequired(true)
            .build();
      }

      String title = session.getValues().containsKey(EVENT_ID_FIELD)
          ? "Edit Event - Step 1 of 3"
          : "Create Event - Step 1 of 3";

      Modal.Builder builder = Modal.create(modalId, title)
          .addComponents(
              Label.of("Template:", templateSelect),
              Label.of("Key hoster:", hosterSelect),
              Label.of("Guild:", guildSelect),
              Label.of("Starting time:", timestampInput),
              Label.of("Duration:", durationSelect)
          );

      return builder.build();
    }

    @Override
    public void submit(ModalInteractionEvent event, StepModalWizardSession session) {
      if (!session.getValues().containsKey(EVENT_ID_FIELD)) {
        String entryId = requiredValue(event, CALENDAR_ENTRY_FIELD);
        var entry = raidCalendarService.findEntry(Long.parseLong(entryId));
        if (!entry.getServerId().equals(Objects.requireNonNull(event.getGuild()).getId())) {
          throw new IllegalArgumentException("The selected calendar entry belongs to another server");
        }
        session.putValue(CALENDAR_ENTRY_FIELD, entryId);
        session.putValue(TEMPLATE_FIELD, entry.getTemplateKey());
        session.putValue(HOSTER_FIELD, entry.getHosterUserId());
        session.putValue(GUILD_FIELD, entry.getGuildName());
        session.putValue(START_FIELD, String.valueOf(entry.getStart().getEpochSecond()));
        session.putValue(DURATION_FIELD, entry.getDuration());
        return;
      }

      session.putValue(TEMPLATE_FIELD, requiredValue(event, TEMPLATE_FIELD));
      session.putValue(HOSTER_FIELD, requiredValue(event, HOSTER_FIELD));
      session.putValue(GUILD_FIELD, requiredValue(event, GUILD_FIELD));
      String timestamp = requiredValue(event, START_FIELD);
      parseTimestamp(timestamp);
      session.putValue(START_FIELD, timestamp);
      session.putValue(DURATION_FIELD, requiredValue(event, DURATION_FIELD));
    }
  }

  private final class DonationAndChannelStep implements StepModalWizardStep {
    @Override
    public String prompt(StepModalWizardSession session) {
      return "Step 2/3: provide the donation and channel information";
    }

    @Override
    public Modal buildModal(String modalId, StepModalWizardSession session, ButtonInteractionEvent event) {
      Server server = serverService.findServerById(
          Objects.requireNonNull(event.getGuild(), "This command is only runnable on a guild.").getId());

      String guildName = session.requireValue(GUILD_FIELD);
      Guild guild = guildApplicationPort.findByServerIdAndName(
          Objects.requireNonNull(event.getGuild(), "This command is only runnable on a guild.").getId(),
          guildName)
          .orElseThrow(() -> new IllegalArgumentException("Guild " + guildName + " not found"));

      EntitySelectMenu categorySelect = EntitySelectMenu.create(CATEGORY_FIELD, EntitySelectMenu.SelectTarget.CHANNEL)
          .setChannelTypes(ChannelType.CATEGORY)
          .build();

      EntitySelectMenu signUpChannelSelect = EntitySelectMenu.create(SIGNUP_FIELD, EntitySelectMenu.SelectTarget.CHANNEL)
          .setChannelTypes(ChannelType.TEXT)
          .build();

      EntitySelectMenu voiceChannelSelect = EntitySelectMenu.create(VOICE_FIELD, EntitySelectMenu.SelectTarget.CHANNEL)
          .setChannelTypes(ChannelType.VOICE)
          .build();

      TextInput.Builder amountBuilder = TextInput.create(DONATION_AMOUNT_FIELD, TextInputStyle.SHORT)
          .setRequired(true)
          .setMaxLength(5)
          .setMinLength(1)
          .setPlaceholder("10");

      RadioGroup donationType = RadioGroup.create(DONATION_TYPE_FIELD)
          .addOption("one-time", "one-time")
          .addOption("per-win", "per-win")
          .setRequired(true)
          .setSelectedValue("one-time")
          .build();

      if (session.getValues().containsKey(DONATION_AMOUNT_FIELD)) {
        amountBuilder.setValue(session.requireValue(DONATION_AMOUNT_FIELD));
      }

      String title = session.getValues().containsKey(EVENT_ID_FIELD)
          ? "Edit Event - Step 2 of 3"
          : "Create Event - Step 2 of 3";
      Modal.Builder builder =  Modal.create(modalId, title);
      if (requiresCategory(server)) {
        builder.addComponents(Label.of("Category:", categorySelect));
      }
      if (!server.isManageSignupChannels()) {
        builder.addComponents(Label.of("Sign-up channel:", signUpChannelSelect));
      }
      if (!server.isManageVoiceChannels()) {
        builder.addComponents(Label.of("Voice channel:", voiceChannelSelect));
      }
      builder.addComponents(
          Label.of("Donation:", amountBuilder.build()),
          Label.of("Donation type:", donationType)
      );
      return builder.build();

    }

    @Override
    public void submit(ModalInteractionEvent event, StepModalWizardSession session) {
      String guildName = session.requireValue(GUILD_FIELD);
      Guild guild = guildApplicationPort.findByServerIdAndName(
          Objects.requireNonNull(event.getGuild(), "This command is only runnable on a guild.").getId(),
          guildName)
          .orElseThrow(() -> new IllegalArgumentException("Guild " + guildName + " not found"));

      Server server  = serverService.findServerById(guild.getServerId());

      if (requiresCategory(server)) {
        session.putValue(CATEGORY_FIELD, requiredValue(event, CATEGORY_FIELD));
      }
      if (!server.isManageSignupChannels()) {
        session.putValue(SIGNUP_FIELD, requiredValue(event, SIGNUP_FIELD));
      }
      if (!server.isManageVoiceChannels()) {
        session.putValue(VOICE_FIELD, requiredValue(event, VOICE_FIELD));
      }

      String amount = requiredValue(event, DONATION_AMOUNT_FIELD);
      parseDonation(amount);
      session.putValue(DONATION_AMOUNT_FIELD, amount);
      session.putValue(DONATION_TYPE_FIELD, requiredValue(event, DONATION_TYPE_FIELD));
    }
  }

  private final class ApprovalStep implements StepModalWizardStep {
    @Override
    public String prompt(StepModalWizardSession session) {
      return "Step 3/3: choose the event admin and whether generated teams need approval.";
    }

    @Override
    public Modal buildModal(String modalId, StepModalWizardSession session, ButtonInteractionEvent event) {
      String eventAdminId = session.getValues().getOrDefault(EVENT_ADMIN_FIELD, session.requireValue(HOSTER_FIELD));
      EntitySelectMenu eventAdminSelect = EntitySelectMenu.create(EVENT_ADMIN_FIELD, EntitySelectMenu.SelectTarget.USER)
          .setRequired(true)
          .setDefaultValues(EntitySelectMenu.DefaultValue.from(api.retrieveUserById(eventAdminId).complete()))
          .build();

      RadioGroup approvalGroup = RadioGroup.create(TEAMS_APPROVAL_FIELD)
          .addOption("No approval", "false")
          .addOption("Require approval", "true")
          .setRequired(true)
          .setSelectedValue(Boolean.parseBoolean(session.getValues().getOrDefault(TEAMS_APPROVAL_FIELD, "false")) ? "true" : "false")
          .build();

      String title = session.getValues().containsKey(EVENT_ID_FIELD)
          ? "Edit Event - Step 3 of 3"
          : "Create Event - Step 3 of 3";

      return Modal.create(modalId, title)
          .addComponents(
              Label.of("Event admin:", eventAdminSelect),
              Label.of("Require team approval:", approvalGroup)
          )
          .build();
    }

    @Override
    public void submit(ModalInteractionEvent event, StepModalWizardSession session) {
      session.putValue(EVENT_ADMIN_FIELD, requiredValue(event, EVENT_ADMIN_FIELD));
      session.putValue(TEAMS_APPROVAL_FIELD, requiredValue(event, TEAMS_APPROVAL_FIELD));
    }
  }

  private boolean requiresCategory(Server server) {
    return server.isManageSignupChannels() || server.isManageVoiceChannels();
  }
}
