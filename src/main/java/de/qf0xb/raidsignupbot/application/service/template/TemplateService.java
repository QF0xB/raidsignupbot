package de.qf0xb.raidsignupbot.application.service.template;

import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@Nonnull
@ApplicationScoped
public class TemplateService {
  private final EventTemplateApplicationPort templateApplicationPort;

  public TemplateService(EventTemplateApplicationPort templateApplicationPort) {
    this.templateApplicationPort = templateApplicationPort;
  }

  @Transactional
  public EventTemplate createTemplate(String key, String name, String details, String emojiSlug) {
    return templateApplicationPort.save(new EventTemplate(key, name, details, emojiSlug));
  }

  @Transactional
  public EventTemplate setName(String key, String name) {
    EventTemplate template = findTemplateByKey(key);
    template.setName(name);
    return templateApplicationPort.save(template);
  }

  @Transactional
  public EventTemplate findTemplateByKey(String key) {
    return templateApplicationPort.findByKey(key).orElseThrow(
        () -> new IllegalArgumentException("Template with key " + key + " not found")
    );
  }

  protected EventTemplate save(EventTemplate template) {
    return templateApplicationPort.save(template);
  }
}
