package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.template;

import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.EditorScreen;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.EditorSession;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class StrategyOverviewScreen implements EditorScreen<EventTemplate> {
  private static final String STRATEGY_KEY = "key";
  private static final String STRATEGY_NAME = "name";
  private static final String STRATEGY_GUIDES = "guides";

  private final String workflowName;
  private String strategyKey;

  public StrategyOverviewScreen(String workflowName, StringSelectInteractionEvent event, EditorSession<EventTemplate> session) {
    this.workflowName = workflowName;

    String strategyKey = event.getValues().getFirst();
    if (strategyKey.equalsIgnoreCase("new")) {
      event.replyModal(buildEditStrategyModal(session, true)).queue();
    } else {
      this.strategyKey = strategyKey;
      log.info("Selected strategy: {}", this.strategyKey);
    }
  }

  @Override
  public String workflowName() {
    return workflowName;
  }

  @Override
  public String id() {
    return "strategy-overview";
  }

  @Override
  public EmbedBuilder embed(EditorSession<EventTemplate> session) {
    if (strategyKey == null) {
      return session.buildEmbedBase("New Strategy").setDescription("Waiting for new strategy to be created.");
    }

    EventStrategy strategy = session.getDraft().getStrategy(strategyKey).orElseThrow(() -> new IllegalStateException("Strategy not found"));

    EmbedBuilder embed = session.buildEmbedBase("Strategy Overview: " + strategy.getName());

    StringBuilder sb = new StringBuilder();
    sb.append("**Key:** " + strategy.getKey() + "\n");
    sb.append("**Name:** " + strategy.getName() + "\n");
    sb.append("**Guides:**\n");
    sb.append(String.join("\n", strategy.getGuides()));
    embed.setDescription(sb.toString());

    return embed;
  }

  @Override
  public List<ActionRow> actionRows(EditorSession<EventTemplate> session) {
    Button editRolesButton = Button.secondary(buildButtonId(session, this, "edit-roles"), "Edit Roles");
    Button editButton = Button.primary(buildButtonId(session, this, "edit-strategy"), "Edit Strategy");

    ArrayList<ActionRow> actionRows = new ArrayList<>();
    actionRows.add(ActionRow.of(editButton, editRolesButton));
    return actionRows;
  }

  @Override
  public boolean onButton(String action, ButtonInteractionEvent event, EditorSession<EventTemplate> session) {
    if (action.equalsIgnoreCase("edit-strategy")) {
      log.info("Edit strategy button pressed");
      event.replyModal(buildEditStrategyModal(session, false)).queue();
      return false;
    }

    if (action.equalsIgnoreCase("edit-roles")) {
      session.push(new RoleListScreen(workflowName, strategyKey));
      return true;
    }

    return false;
  }

  @Override
  public boolean onModal(String action, ModalInteractionEvent event, EditorSession<EventTemplate> session) {
    log.info("Modal interaction in StrategyOverviewScreen: {}", action);
    if (action.equalsIgnoreCase("edit-strategy")) {
      EventStrategy strategy;

      session.draftChanged();

      if (strategyKey == null) {
        String key = Objects.requireNonNull(event.getValue(STRATEGY_KEY)).getAsString();
        String name = Objects.requireNonNull(event.getValue(STRATEGY_NAME)).getAsString();

        strategy = new EventStrategy(key, name);
        session.getDraft().addStrategy(strategy);
      } else {
        strategy = session.getDraft().getStrategy(strategyKey).orElseThrow(() -> new IllegalStateException("Strategy not found"));
        strategy.setName(Objects.requireNonNull(event.getValue(STRATEGY_NAME)).getAsString());
      }

      String guides = Objects.requireNonNull(event.getValue(STRATEGY_GUIDES)).getAsString();
      Arrays.stream(guides.split("\n")).forEach(strategy::addGuide);

      return true;
    }

    return false;
  }

  private Modal buildEditStrategyModal(EditorSession<EventTemplate> session, boolean isNew) {
    Modal.Builder modalBuilder = Modal.create(buildModalId(session, this, "edit-strategy"), isNew ? "New " : "Edit " + "Strategy");

    log.info("Building modal for strategy overview {}", modalBuilder.getId());
    TextInput keyInput = TextInput.create(STRATEGY_KEY, TextInputStyle.SHORT)
        .setRequired(true)
        .setMaxLength(255)
        .build();

    TextInput.Builder nameInputBuilder = TextInput.create(STRATEGY_NAME, TextInputStyle.SHORT)
        .setRequired(true)
        .setMaxLength(255);

    TextInput.Builder guideInputBuilder = TextInput.create(STRATEGY_GUIDES, TextInputStyle.PARAGRAPH)
        .setRequired(false);

    if (isNew) {
      modalBuilder.addComponents(
          Label.of("Strategy key:", keyInput).withDescription("Must be unique per Event. Make sure it doesn't exist twice in the same event!")
      );
    } else {
      EventStrategy strategy = session.getDraft().getStrategy(strategyKey).orElseThrow(() -> new IllegalStateException("Strategy not found"));
      nameInputBuilder.setValue(strategy.getName());
      if (!strategy.getGuides().isEmpty()) {
        guideInputBuilder.setValue(String.join("\n", strategy.getGuides()));
      }
    }

    modalBuilder.addComponents(
        Label.of("Strategy name:", nameInputBuilder.build()),
        Label.of("Strategy guides:", guideInputBuilder.build()).withDescription("One Link per line.")
    );

    return modalBuilder.build();
  }


}
