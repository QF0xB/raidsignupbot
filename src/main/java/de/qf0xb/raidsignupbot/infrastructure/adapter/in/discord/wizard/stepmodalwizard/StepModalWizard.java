package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard;

import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.SlashCommandDefinition;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.ComponentInteractionHandler;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class StepModalWizard implements ComponentInteractionHandler, SlashCommandDefinition {
  protected static final Duration DEFAULT_SESSION_TTL = Duration.ofMinutes(15);
  private static final String CONTINUE_ACTION = "continue";
  private static final String CANCEL_ACTION = "cancel";

  protected final List<StepModalWizardStep> steps = new ArrayList<>();
  protected final Map<String, StepModalWizardSession> sessions = new ConcurrentHashMap<>();

  private final String commandName;
  private final String commandDescription;
  private final CommandAccessLevel accessLevel;

  public StepModalWizard(String commandName, String commandDescription) {
    this(commandName, commandDescription, CommandAccessLevel.PUBLIC);
  }

  public StepModalWizard(String commandName, String commandDescription, CommandAccessLevel accessLevel) {
    this.commandName = commandName;
    this.commandDescription = commandDescription;
    this.accessLevel = accessLevel;
  }

  public abstract String onComplete(StepModalWizardSession session, ModalInteractionEvent event);

  public void addStep(StepModalWizardStep step) {
    steps.add(step);
  }

  @Override
  public boolean supportsButtonId(String buttonId) {
    return parsedButtonId(buttonId).isPresent();
  }

  @Override
  public void onButtonInteraction(ButtonInteractionEvent event) {
    ParsedButtonId parsed = parsedButtonId(event.getButton().getCustomId()).orElse(null);
    if (parsed == null) {
      return;
    }

    StepModalWizardSession session = sessions.get(parsed.sessionId());
    if (!validateSession(event, session, parsed.stepIndex())) {
      return;
    }

    if (CANCEL_ACTION.equalsIgnoreCase(parsed.action())) {
      sessions.remove(session.getSessionId());
      event.deferEdit()
          .queue(ignored -> finishPrompt(session, "Wizard cancelled."));
      return;
    }

    if (!CONTINUE_ACTION.equalsIgnoreCase(parsed.action)) {
      return;
    }

    try {
      event.replyModal(currentStep(session).buildModal(buildModalId(session), session, event)).queue();
    } catch (RuntimeException ex) {
      event.reply("Failed to open wizard step: " + ex.getMessage())
          .setEphemeral(true)
          .queue();
    }
  }

  @Override
  public boolean supportsModalId(String modalId) {
    return parsedModalId(modalId).isPresent();
  }

  @Override
  public void onModalInteraction(ModalInteractionEvent event) {
    ParsedModalId parsed = parsedModalId(event.getModalId()).orElse(null);
    if (parsed == null) {
      return;
    }

    StepModalWizardSession session = sessions.get(parsed.sessionId());
    if (!validateSession(event, session, parsed.stepIndex())) {
      return;
    }

    try {
      currentStep(session).submit(event, session);
      session.touch(DEFAULT_SESSION_TTL);
    } catch (RuntimeException ex) {
      event.reply(ex.getMessage())
          .setEphemeral(true)
          .queue();
      return;
    }

    if (session.getStepIndex() + 1 < steps.size()) {
      session.advanceStep();
      try {
        event.deferEdit().queue(ignored -> sendPrompt(event, session));
      } catch (RuntimeException ex) {
        sessions.remove(session.getSessionId());
        session.clear();
        event.reply("Failed to continue wizard: " + ex.getMessage())
            .setEphemeral(true)
            .queue();
      }
      return;
    }

    sessions.remove(session.getSessionId());
    event.deferEdit().complete();
    finishPrompt(session, onComplete(session, event));
  }

  @Override
  public String getCommandName() {
    return this.commandName;
  }

  @Override
  public String getDescription() {
    return this.commandDescription;
  }

  @Override
  public CommandAccessLevel getAccessLevel() {
    return accessLevel;
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    cleanupExpiredSessions();

    if(steps.isEmpty()) {
      event.reply("This wizard is not configured yet.")
          .setEphemeral(true)
          .queue();
      return;
    }

    StepModalWizardSession session = createSession(event.getUser().getId());
    registerSession(session);

    try {
      sendPrompt(event, session);
    } catch (RuntimeException ex) {
      sessions.remove(session.getSessionId());
      event.reply("Failed to start wizard: " + ex.getMessage())
          .setEphemeral(true)
          .queue();
    }
  }

  @Override
  public void autoComplete(CommandAutoCompleteInteractionEvent event) {
    // wizard commands don't use slash auto-complete'
  }

  public String requiredValue(ModalInteractionEvent event, String fieldName) {
    ModalMapping mapping = event.getValue(fieldName);
    if (mapping == null) {
      throw new IllegalArgumentException("Missing modal field: " + fieldName);
    }

    return switch (mapping.getType()) {
      case TEXT_INPUT, RADIO_GROUP -> mapping.getAsString();
      case STRING_SELECT, MENTIONABLE_SELECT, USER_SELECT, ROLE_SELECT, CHANNEL_SELECT -> mapping.getAsStringList().getFirst();
      default -> throw new IllegalArgumentException("Unsupported mapping type: " + mapping.getType());
    };
  }

  protected StepModalWizardSession createSession(String userId) {
    return new StepModalWizardSession(userId, DEFAULT_SESSION_TTL);
  }

  protected void registerSession(StepModalWizardSession session) {
    sessions.put(session.getSessionId(), session);
  }

  protected void removeSession(StepModalWizardSession session) {
    sessions.remove(session.getSessionId());
  }

  protected void sendPrompt(IReplyCallback event, StepModalWizardSession session) {
    StepModalWizardStep step = currentStep(session);

    if (session.getInteractionHook() != null) {
      InteractionHook hook = session.getInteractionHook();
      hook.editOriginal(step.prompt(session))
          .setComponents(buildButtons(session))
          .queue();
    } else {
      event.reply(step.prompt(session))
          .addComponents(buildButtons(session))
          .queue(session::setInteractionHook);
    }
  }

  protected void finishPrompt(StepModalWizardSession session, String completionMessage) {
    InteractionHook hook = session.getInteractionHook();
    hook.editOriginal(completionMessage)
        .setComponents(List.of())
        .queue();
  }

  private ActionRow buildButtons(StepModalWizardSession session) {
    Button cancelButton = Button.danger(buildButtonId(session, CANCEL_ACTION), "Cancel");
    Button continueButton = Button.primary(buildButtonId(session, CONTINUE_ACTION), "Continue");

    return ActionRow.of(cancelButton, continueButton);
  }

  private String buildButtonId(StepModalWizardSession session, String action) {
    return buildModalId(session) + ":" + action;
  }

  private String buildModalId(StepModalWizardSession session) {
    return buildModalId(session, session.getStepIndex());
  }

  private String buildModalId(StepModalWizardSession session, int stepIndex) {
    return commandName + ":" + session.getSessionId() + ":" + stepIndex;
  }

  private StepModalWizardStep currentStep(StepModalWizardSession session) {
    return steps.get(session.getStepIndex());
  }

  @Override
  public boolean hasName(String name) {
    return commandName.equalsIgnoreCase(name.strip());
  }

  private boolean validateSession(IReplyCallback event, StepModalWizardSession session, int stepIndex) {
    if (session == null) {
      event.reply("This wizard session has expired or was already completed.")
          .setEphemeral(true)
          .queue();
      return false;
    }

    if (session.isExpired()) {
      sessions.remove(session.getSessionId());
      event.reply("This wizard session has expired.")
          .setEphemeral(true)
          .queue();
      session.clear();
      return false;
    }

    if (!Objects.equals(session.getUserId(), event.getUser().getId())) {
      event.reply("This wizard session belongs to another user.")
          .setEphemeral(true)
          .queue();
      return false;
    }

    if (session.getStepIndex() != stepIndex) {
      event.reply("This wizard step is out of date. Please reopen the wizard.")
          .setEphemeral(true)
          .queue();
      return false;
    }

    return true;
  }

  public void cleanupExpiredSessions() {
    sessions.entrySet().stream()
        .filter(entry -> entry.getValue().isExpired())
            .forEach(stringModalWizardSessionEntry -> stringModalWizardSessionEntry.getValue().clear());

    sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
  }

  private Optional<ParsedButtonId> parsedButtonId(String buttonId) {
    if (buttonId == null || !buttonId.startsWith(commandName + ":")) {
      return Optional.empty();
    }

    String[] parts = buttonId.split(":", 4);
    if (parts.length != 4) {
      return Optional.empty();
    }

    try {
      return Optional.of(new ParsedButtonId(parts[1], Integer.parseInt(parts[2]), parts[3]));
    } catch (NumberFormatException ex) {
      return Optional.empty();
    }
  }

  private Optional<ParsedModalId> parsedModalId(String modalId) {
    if (modalId == null || !modalId.startsWith(commandName + ":")) {
      return Optional.empty();
    }

    String[] parts = modalId.split(":", 3);
    if (parts.length != 3) {
      return Optional.empty();
    }

    try {
      return Optional.of(new ParsedModalId(parts[1], Integer.parseInt(parts[2])));
    } catch (NumberFormatException ex) {
      return Optional.empty();
    }
  }

  @Override
  public Collection<? extends OptionData> getOptions() {
    return List.of();
  }

  private record ParsedButtonId(String sessionId, int stepIndex, String action) {}
  private record ParsedModalId(String sessionId, int stepIndex) {}
}
