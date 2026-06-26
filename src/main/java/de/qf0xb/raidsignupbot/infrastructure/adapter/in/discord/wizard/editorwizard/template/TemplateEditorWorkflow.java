package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.template;

import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.EditorScreen;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.EditorSession;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.EditorWorkflow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;

@ApplicationScoped
public class TemplateEditorWorkflow extends EditorWorkflow<EventTemplate> {
  @Inject EventTemplateApplicationPort eventTemplateApplicationPort;

  public TemplateEditorWorkflow() {
    super("template-editor");
  }

  @Override
  public EditorScreen<EventTemplate> initialScreen(EditorSession<EventTemplate> session) {
    return new TemplateOverviewScreen(getWorkflowName());
  }

  @Override
  public EmbedBuilder onFinish(EditorSession<EventTemplate> session) {
    EmbedBuilder finish = buildEmbedBase("Template saved", session);

    try {
      eventTemplateApplicationPort.save(session.getDraft());
      finish.setColor(Color.green);
      finish.setDescription("Template saved successfully.");
    } catch (Exception e) {
      finish.setColor(Color.red);
      finish.setDescription("Failed to save template: " + e.getMessage());
    }

    return finish;
  }
}
