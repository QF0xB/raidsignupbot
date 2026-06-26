package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.template;

import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.EditorScreen;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.EditorSession;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.radiogroup.RadioGroup;
import net.dv8tion.jda.api.components.radiogroup.RadioGroupOption;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class RoleOverviewScreen implements EditorScreen<EventTemplate> {
  private static final String ROLE_TYPE = "roleType";
  private final String ROLE_KEY = "roleKey";
  private final String ROLE_NAME = "roleName";
  private final String ROLE_EMOJI = "roleEmoji";
  private final String ROLE_COUNT = "roleCount";
  private final String ROLE_ELIGIBLE_PRIMARY_ROLES = "roleEligiblePrimaryRoles";

  private final String workflowName;
  private final String strategyKey;
  private String roleKey;

  private boolean isUtility;

  public RoleOverviewScreen(
      String workflowName,
      String strategyKey,
      StringSelectInteractionEvent event,
      EditorSession<EventTemplate> session) {
    this.workflowName = workflowName;
    this.strategyKey = strategyKey;

    String roleKey = event.getValues().getFirst();
    if (roleKey.equalsIgnoreCase("new-utility")) {
      isUtility = true;
      event.replyModal(buildEditStrategyModal(session, true)).queue();
    } else if (roleKey.equalsIgnoreCase("new-primary")) {
      event.replyModal(buildEditStrategyModal(session, true)).queue();
    } else {
      this.roleKey = roleKey;
    }
  }

  @Override
  public String workflowName() {
    return workflowName;
  }

  @Override
  public String id() {
    return "role-overview";
  }

  @Override
  public EmbedBuilder embed(EditorSession<EventTemplate> session) {
    if (roleKey == null) {
      return session
          .buildEmbedBase("New Role")
          .setDescription("Waiting for new role to be created.");
    }

    EventRole role =
        session
            .getDraft()
            .getStrategy(strategyKey)
            .orElseThrow(() -> new IllegalStateException("Strategy not found"))
            .findRole(roleKey)
            .orElseThrow(() -> new IllegalStateException("Role not found"));
    EmbedBuilder embed = session.buildEmbedBase("Role Overview: " + role.getName());
    StringBuilder sb = new StringBuilder();
    sb.append("**Key:** " + role.getKey() + "\n");
    sb.append("**Name:** " + role.getName() + "\n");
    sb.append("**Emoji:** " + role.getEmojiSlug() + "\n");
    sb.append("**Required count:** " + role.getRequiredCount() + "\n");

    if (role instanceof EventUtilityRole utilityRole) {
      sb.append(
          "**Eligible primary roles:** "
              + String.join(", ", utilityRole.getEligiblePrimaryRoleKeys())
              + "\n");
    }

    embed.setDescription(sb.toString());
    return embed;
  }

  @Override
  public List<ActionRow> actionRows(EditorSession<EventTemplate> session) {
    if (roleKey == null) {
      return new ArrayList<>();
    }

    Button editButton = Button.primary(buildButtonId(session, this, "edit-role"), "Edit Role");
    ArrayList<ActionRow> actionRows = new ArrayList<>();
    actionRows.add(ActionRow.of(editButton));
    return actionRows;
  }

  @Override
  public boolean onButton(
      String action, ButtonInteractionEvent event, EditorSession<EventTemplate> session) {
    if (action.equalsIgnoreCase("edit-role")) {
      event.replyModal(buildEditStrategyModal(session, false)).queue();
      return false;
    }

    return false;
  }

  @Override
  public boolean onModal(
      String action, ModalInteractionEvent event, EditorSession<EventTemplate> session) {
    if (action.equalsIgnoreCase("edit-role")) {
      EventRole role;

      session.draftChanged();

      String name = Objects.requireNonNull(event.getValue(ROLE_NAME)).getAsString();
      String emoji = Objects.requireNonNull(event.getValue(ROLE_EMOJI)).getAsString();
      int count = 0;

      try {
        count = Integer.parseInt(Objects.requireNonNull(event.getValue(ROLE_COUNT)).getAsString());
      } catch (NumberFormatException ex) {
        event.reply("Invalid role count").setEphemeral(true).queue();
        return false;
      }

      if (roleKey == null) {
        String key = Objects.requireNonNull(event.getValue(ROLE_KEY)).getAsString();

        if (isUtility) {
          role = new EventUtilityRole(key, name, emoji, count);
        } else {
          role = new EventRole(key, name, emoji, count);
        }
      } else {
        role =
            session
                .getDraft()
                .getStrategy(strategyKey)
                .orElseThrow(() -> new IllegalStateException("Strategy not found"))
                .findRole(roleKey)
                .orElseThrow(() -> new IllegalStateException("Role not found"));
        role.setName(name);
        role.setEmojiSlug(emoji);
        role.setRequiredCount(count);
      }

      if (role instanceof EventUtilityRole utilityRole) {
        List<String> eligiblePrimaryRoleKeys =
            Objects.requireNonNull(event.getValue(ROLE_ELIGIBLE_PRIMARY_ROLES)).getAsStringList();
        utilityRole.clearEligiblePrimaryRoles();
        eligiblePrimaryRoleKeys.forEach(utilityRole::addEligiblePrimaryRole);
      }

      return true;
    }

    return false;
  }

  private Modal buildEditStrategyModal(EditorSession<EventTemplate> session, boolean isNew) {
    Modal.Builder modalBuilder =
        Modal.create(buildModalId(session, this, "edit-role"), isNew ? "New " : "Edit " + "Role");
    EventStrategy strategy =
        session
            .getDraft()
            .getStrategy(strategyKey)
            .orElseThrow(() -> new IllegalStateException("Strategy not found"));

    log.info("Building modal for role edit {}", modalBuilder.getId());

    TextInput keyInput =
        TextInput.create(ROLE_KEY, TextInputStyle.SHORT)
            .setRequired(true)
            .setMaxLength(255)
            .build();

    TextInput.Builder nameInputBuilder =
        TextInput.create(ROLE_NAME, TextInputStyle.SHORT).setRequired(true).setMaxLength(255);

    TextInput.Builder roleEmojiBuilder =
        TextInput.create(ROLE_EMOJI, TextInputStyle.SHORT).setRequired(true).setMaxLength(255);

    TextInput.Builder roleCountBuilder =
        TextInput.create(ROLE_COUNT, TextInputStyle.SHORT).setRequired(true);

    StringSelectMenu.Builder eligibleRolesBuilder =
        StringSelectMenu.create(ROLE_ELIGIBLE_PRIMARY_ROLES)
            .addOptions(
                strategy.getPrimaryRoles().stream()
                    .filter(eventRole -> !eventRole.getKey().equalsIgnoreCase(roleKey))
                    .map(eventRole -> SelectOption.of(eventRole.getKey(), eventRole.getKey()))
                    .toList())
            .setMinValues(0)
            .setRequired(false)
            .setMaxValues(strategy.getPrimaryRoles().size());

    log.info("Buìlt eligibleRoles with options {}" , eligibleRolesBuilder.getOptions());

    if (isNew) {
      roleCountBuilder.setValue("1");
      modalBuilder.addComponents(
          Label.of("Role key:", keyInput)
              .withDescription(
                  "Must be unique per Strategy. Make sure it doesn't exist twice in the same event!"));

      if (isUtility) {
        if (!eligibleRolesBuilder.getOptions().isEmpty()) {
          modalBuilder.addComponents(
              Label.of("Eligible primary roles:", eligibleRolesBuilder.build())
                  .withDescription("The roles that can be assigned to this role. If no role is set here, every role is allowed."));
        }
      }

    } else {
      EventRole role =
          strategy.findRole(roleKey).orElseThrow(() -> new IllegalStateException("Role not found"));
      nameInputBuilder.setValue(strategy.getName());

      if (role instanceof EventUtilityRole utilityRole) {
        eligibleRolesBuilder.setDefaultValues(utilityRole.getEligiblePrimaryRoleKeys());
        isUtility = true;

        if (!eligibleRolesBuilder.getOptions().isEmpty()) {
          modalBuilder.addComponents(
              Label.of("Eligible primary roles:", eligibleRolesBuilder.build())
                  .withDescription("The roles that can be assigned to this role. If no role is set here, every role is allowed."));
        }
      }

      roleEmojiBuilder.setValue(role.getEmojiSlug());
      roleCountBuilder.setValue(String.valueOf(role.getRequiredCount()));
    }
    modalBuilder.addComponents(
        Label.of("Role name", nameInputBuilder.build()),
        Label.of("Role emoji", roleEmojiBuilder.build())
            .withDescription("Must be unique per Event, its the reaction role."),
        Label.of("Role count:", roleCountBuilder.build())
            .withDescription("How many times this role needs to be assigned."));

    return modalBuilder.build();
  }
}
