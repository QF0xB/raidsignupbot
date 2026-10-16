package de.qf0xb.raidsignupbot.application.service.template;

import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@Nonnull
@ApplicationScoped
public class TemplateService {
  private final EventTemplateApplicationPort templateApplicationPort;
  private final ServerService serverService;

  public TemplateService(EventTemplateApplicationPort templateApplicationPort, ServerService serverService) {
    this.templateApplicationPort = templateApplicationPort;
    this.serverService = serverService;
  }

  @Transactional
  public EventTemplate createTemplate(String serverId, String key, String name, String details, String emojiSlug) {
    EventTemplate template = new EventTemplate(key, name, details, emojiSlug);
    template.setServer(serverService.ensureServer(serverId));
    return templateApplicationPort.save(template);
  }

  @Transactional
  public EventTemplate setName(String serverId, String key, String name) {
    EventTemplate template = findTemplateByKey(serverId, key);
    template.setName(name);
    return templateApplicationPort.save(template);
  }

  @Transactional
  public EventTemplate updateTemplate(String serverId, String key, String name, String emojiSlug, String details) {
    EventTemplate template = findTemplateByKey(serverId, key);
    template.setName(name);
    template.setEmojiSlug(emojiSlug);
    template.setDetails(details);
    return templateApplicationPort.save(template);
  }

  @Transactional
  public EventTemplate findTemplateByKey(String serverId, String key) {
    return templateApplicationPort.findByServerIdAndKey(serverId, key).orElseThrow(
        () -> new IllegalArgumentException("Template with key " + key + " not found for server " + serverId)
    );
  }

  @Transactional
  public java.util.Set<EventTemplate> findAllTemplates(String serverId) {
    return templateApplicationPort.findAllByServerId(serverId);
  }

  protected EventTemplate save(EventTemplate template) {
    return templateApplicationPort.save(template);
  }
}
