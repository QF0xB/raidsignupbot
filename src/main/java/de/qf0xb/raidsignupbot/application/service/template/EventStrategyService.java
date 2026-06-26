package de.qf0xb.raidsignupbot.application.service.template;

import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Set;

@ApplicationScoped
public class EventStrategyService {
  private final TemplateService templateService;

  public EventStrategyService(TemplateService templateService) {
    this.templateService = templateService;
  }


  @Transactional
  public EventTemplate addStrategy(String serverId, String templateKey, String strategyKey, String name) {
    EventTemplate template = templateService.findTemplateByKey(serverId, templateKey);
    template.addStrategy(new EventStrategy(strategyKey, name));
    return templateService.save(template);
  }

  @Transactional
  public EventTemplate removeStrategy(String serverId, String templateKey, String strategyKey) {
    EventTemplate template = templateService.findTemplateByKey(serverId, templateKey);
    template.removeStrategy(strategyKey);
    return templateService.save(template);
  }

  @Transactional
  public EventTemplate addPrimaryRole(String serverId, String templateKey, String strategyKey, String roleKey, String name, String emojiSlug, int requiredCount) {
    EventTemplate template = templateService.findTemplateByKey(serverId, templateKey);
    EventStrategy strategy = template.getStrategy(strategyKey).orElseThrow(
        () -> new IllegalArgumentException("Strategy with key " + strategyKey + " not found")
    );

    strategy.addRole(new EventRole(roleKey, name, emojiSlug, requiredCount));
    return templateService.save(template);
  }

  @Transactional
  public EventTemplate addUtilityRole(String serverId, String templateKey, String strategyKey, String roleKey, String name, String emojiSlug, int requiredCount, Set<String> eligiblePrimaryRoleKeys) {
    EventTemplate template = templateService.findTemplateByKey(serverId, templateKey);
    EventStrategy strategy = template.getStrategy(strategyKey).orElseThrow(
        () -> new IllegalArgumentException("Strategy with key " + strategyKey + " not found")
    );

    EventUtilityRole role = new EventUtilityRole(roleKey, name, emojiSlug, requiredCount);
    eligiblePrimaryRoleKeys.forEach(role::addEligiblePrimaryRole);
    strategy.addRole(role);
    return templateService.save(template);
  }

  @Transactional
  public EventTemplate removeRole(String serverId, String templateKey, String strategyKey, String roleKey) {
    EventTemplate template = templateService.findTemplateByKey(serverId, templateKey);
    EventStrategy strategy = template.getStrategy(strategyKey).orElseThrow(
        () -> new IllegalArgumentException("Strategy with key " + strategyKey + " not found")
    );

    strategy.removeRole(roleKey);
    return templateService.save(template);
  }
}
