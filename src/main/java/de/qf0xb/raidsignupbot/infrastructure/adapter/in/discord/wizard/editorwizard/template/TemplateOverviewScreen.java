package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.template;

import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.EditorScreen;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.EditorSession;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public class TemplateOverviewScreen implements EditorScreen<EventTemplate> {
  private static final String TEMPLATE_NAME_KEY = "name";
  private static final String TEMPLATE_EMOJI_KEY = "emoji";
  private static final String TEMPLATE_DETAILS_KEY = "details";

  private final String workflowName;

  public TemplateOverviewScreen(String workflowName) {
    this.workflowName = workflowName;
  }

  @Override
  public String workflowName() {
    return workflowName;
  }

  public String id() {
    return "template-overview";
  }

  @Override
  public EmbedBuilder embed(EditorSession<EventTemplate> session) {
    String title =
        String.format("Template Editor: Editing template \"%s\"", session.getDraft().getKey());
    EmbedBuilder embed = session.buildEmbedBase(title);

    String contentBuilder = "**Name:** " + session.getDraft().getName() + "\n" +
        "**Emoji:** " + session.getDraft().getEmojiSlug() + "\n" +
        "**Details:** " + session.getDraft().getDetails() + "\n";
    embed.setDescription(contentBuilder);

    return embed;
  }

  @Override
  public List<ActionRow> actionRows(EditorSession<EventTemplate> session) {
    ActionRow actionRow = ActionRow.of(
        Button.primary(buildButtonId(session, this, "edit-strategies"), "Edit Strategies"),
        Button.secondary(buildButtonId(session, this, "edit-template"), "Edit Template")
    );

    System.out.println(actionRow);

    List<ActionRow> actionRows = new ArrayList<>();
    actionRows.add(actionRow);
    return actionRows;
  }

  @Override
  public boolean onButton(String action, ButtonInteractionEvent event, EditorSession<EventTemplate> session) {
    if (action.equalsIgnoreCase("edit-template")) {
      Modal modal = buildEditTemplateModal(session);
      event.replyModal(modal).queue();
      return false;
    } else if (action.equalsIgnoreCase("edit-strategies")) {
      session.push(new StrategyListScreen(workflowName));
      return true;
    }

    throw new UnsupportedOperationException("Action " + action + " is not supported.");
  }

  private Modal buildEditTemplateModal(EditorSession<EventTemplate> session) {
    Modal.Builder modalBuilder = Modal.create(buildModalId(session, session.current(), "edit-template"), "Edit Template \"" + session.getDraft().getKey() + "\"");

    TextInput nameInput = TextInput.create(TEMPLATE_NAME_KEY, TextInputStyle.SHORT)
        .setValue(session.getDraft().getName())
        .setMaxLength(255)
        .setRequired(true)
        .build();

    TextInput emojiInput = TextInput.create(TEMPLATE_EMOJI_KEY, TextInputStyle.SHORT)
        .setValue(session.getDraft().getEmojiSlug())
        .setRequired(true)
        .setMaxLength(255)
        .build();

    TextInput descriptionInput = TextInput.create(TEMPLATE_DETAILS_KEY, TextInputStyle.PARAGRAPH)
        .setValue(session.getDraft().getDetails())
        .setRequired(true)
        .build();

    modalBuilder.addComponents(
        Label.of("Template name:", nameInput),
        Label.of("Emoji slug:", emojiInput).withDescription("Without starting and ending colons (:)"),
        Label.of("Description:", descriptionInput)
    );

    return modalBuilder.build();
  }

  @Override
  public boolean onModal(String action, ModalInteractionEvent event, EditorSession<EventTemplate> session) {
    if (action.equalsIgnoreCase("edit-template")) {
      session.getDraft().setName(Objects.requireNonNull(event.getValue(TEMPLATE_NAME_KEY)).getAsString());
      session.getDraft().setEmojiSlug(Objects.requireNonNull(event.getValue(TEMPLATE_EMOJI_KEY)).getAsString());
      session.getDraft().setDetails(Objects.requireNonNull(event.getValue(TEMPLATE_DETAILS_KEY)).getAsString());
      session.draftChanged();
    }

    return false;
  }
}
