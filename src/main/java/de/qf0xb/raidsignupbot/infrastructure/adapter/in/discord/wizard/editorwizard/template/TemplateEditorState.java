package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.template;

import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
public class TemplateEditorState {
  private final Map<String, EventTemplate> templates;

  private String templateKey;
  private String strategyKey;
  private String roleKey;

  private boolean dirty;

  public TemplateEditorState(Map<String, EventTemplate> templates) {
    this.templates = templates;
  }

  public void setTemplateKey(String templateKey) {
    this.templateKey = templateKey;
    this.strategyKey = null;
    this.roleKey = null;
  }

  public void setStrategyKey(String strategyKey) {
    if (templateKey == null) {
      throw new IllegalStateException("Template key must be set before setting strategy key");
    }
    this.strategyKey = strategyKey;
    this.roleKey = null;
  }

  public void setRoleKey(String roleKey) {
    if (strategyKey == null || templateKey == null) {
      throw new IllegalStateException("Strategy and template key must be set before setting role key");
    }
    this.roleKey = roleKey;
  }

  public void markDirty() {
    dirty = true;
  }
}
