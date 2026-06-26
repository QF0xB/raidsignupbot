package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.template;

import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.EditorScreen;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.EditorSession;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

import java.util.ArrayList;
import java.util.List;

public class RoleListScreen implements EditorScreen<EventTemplate> {
  private final String workflowName;
  private final String strategyName;

  public RoleListScreen(String workflowName, String strategyName) {
    this.workflowName = workflowName;
    this.strategyName = strategyName;
  }

  @Override
  public String workflowName() {
    return workflowName;
  }

  @Override
  public String id() {
    return "role-list";
  }

  @Override
  public EmbedBuilder embed(EditorSession<EventTemplate> session) {
    EventStrategy strategy = session.getDraft().getStrategy(strategyName).orElseThrow(() -> new IllegalStateException("Strategy not found"));

    EmbedBuilder embed = session.buildEmbedBase("Edit Roles for Strategy " + strategy.getKey());

    StringBuilder sb = new StringBuilder();
    sb.append("**Primary roles:**\n");
    strategy.getPrimaryRoles().forEach(role -> sb.append(role.getKey()).append(" | ").append(role.getName()).append("\n"));
    sb.append("\n**Utility roles:**\n");
    strategy.getUtilityRoles().forEach(role -> {
      sb.append(role.getKey()).append(" | ").append(role.getName()).append(" with allowed roles (");
      sb.append(String.join(", ", role.getEligiblePrimaryRoleKeys()));
      sb.append(")\n");
    });

    embed.setDescription(sb.toString());

    return embed;
  }

  @Override
  public List<ActionRow> actionRows(EditorSession<EventTemplate> session) {
    EventStrategy strategy = session.getDraft().getStrategy(strategyName).orElseThrow(() -> new IllegalStateException("Strategy not found"));

    StringSelectMenu strategySelect =
        StringSelectMenu.create(buildStringSelectId(session, this, "role-select"))
            .addOptions(
                strategy.getPrimaryRoles().stream()
                    .map(role -> SelectOption.of(role.getKey(), role.getKey()).withDescription("Primary role")).toList())
            .addOptions(strategy.getUtilityRoles().stream()
                .map(role -> SelectOption.of(role.getKey(), role.getKey()).withDescription("UtilityRole")).toList())
            .addOptions(SelectOption.of("New Primary Role", "new-primary").withDescription("Create new primary role"))
            .addOptions(SelectOption.of("New Utility Role", "new-utility").withDescription("Create new utility role"))
            .build();

    List<ActionRow> actionRows = new ArrayList<>();
    ActionRow actionRow = ActionRow.of(strategySelect);
    actionRows.add(actionRow);
    return actionRows;
  }

  @Override
  public boolean onStringSelect(String action, StringSelectInteractionEvent event, EditorSession<EventTemplate> session) {
    if (action.equalsIgnoreCase("role-select")) {
      String roleKey = event.getValues().getFirst();

      session.push(new RoleOverviewScreen(workflowName, strategyName, event, session));

      return !roleKey.equalsIgnoreCase("new-primary") && !roleKey.equalsIgnoreCase("new-utility");
    }

    return false;
  }
}
