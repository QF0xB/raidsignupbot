package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.template;

import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.EditorScreen;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.EditorSession;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class StrategyListScreen implements EditorScreen<EventTemplate> {
  private final String workflowName;

  public StrategyListScreen(String workflowName) {
    this.workflowName = workflowName;
  }

  @Override
  public String workflowName() {
    return workflowName;
  }

  @Override
  public String id() {
    return "strategy-list";
  }

  @Override
  public EmbedBuilder embed(EditorSession<EventTemplate> session) {
    EmbedBuilder embed = session.buildEmbedBase("Strategies:");

    StringBuilder sb = new StringBuilder();
    sb.append("**Existing strategies:**\n");
    session
        .getDraft()
        .getStrategies()
        .forEach(strategy -> sb.append(strategy.getKey()).append(" | ").append(strategy.getName()).append("\n"));

    embed.setDescription(sb.toString());
    return embed;
  }

  @Override
  public List<ActionRow> actionRows(EditorSession<EventTemplate> session) {
    StringSelectMenu strategySelect =
        StringSelectMenu.create(buildStringSelectId(session, this, "strategy-select"))
            .addOptions(
                session.getDraft().getStrategies().stream()
                    .map(
                        eventStrategy ->
                            SelectOption.of(eventStrategy.getKey(), eventStrategy.getKey())
                                .withDescription(eventStrategy.getName()))
                    .toList())
            .addOptions(SelectOption.of("New", "new").withDescription("Create new strategy"))
            .build();

    ArrayList<ActionRow> actionRows = new ArrayList<>();
    actionRows.add(ActionRow.of(strategySelect));
    return actionRows;
  }

  @Override
  public boolean onStringSelect(String action, StringSelectInteractionEvent event, EditorSession<EventTemplate> session) {
    if (action.equalsIgnoreCase("strategy-select")) {
      log.info("Selected strategy: {}", event.getValues().getFirst());


      session.push(new StrategyOverviewScreen(workflowName, event, session));


      String stringSelectValue = event.getValues().getFirst();
      if (stringSelectValue.equalsIgnoreCase("new")) {
        return false;
      } else {
        return true;
      }
    }

    return false;
  }




}
