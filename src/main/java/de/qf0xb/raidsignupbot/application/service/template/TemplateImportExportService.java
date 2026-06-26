package de.qf0xb.raidsignupbot.application.service.template;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.qf0xb.raidsignupbot.application.dto.template.TemplateBundle;
import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class TemplateImportExportService {
  private final EventTemplateApplicationPort templateApplicationPort;
  private final ServerService serverService;
  private final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules()
      .enable(SerializationFeature.INDENT_OUTPUT);

  public TemplateImportExportService(EventTemplateApplicationPort templateApplicationPort, ServerService serverService) {
    this.templateApplicationPort = templateApplicationPort;
    this.serverService = serverService;
  }

  public byte[] exportTemplates(Set<EventTemplate> templates) {
    try {
      return objectMapper.writeValueAsBytes(toBundle(templates));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Could not export templates", e);
    }
  }

  public String exportTemplatesAsString(Set<EventTemplate> templates) {
    return new String(exportTemplates(templates), StandardCharsets.UTF_8);
  }

  public byte[] exportTemplate(EventTemplate template) {
    return exportTemplates(Set.of(template));
  }

  public String exportTemplateAsString(EventTemplate template) {
    return new String(exportTemplate(template), StandardCharsets.UTF_8);
  }

  public List<EventTemplate> importTemplates(String serverId, InputStream inputStream) {
    try {
      TemplateBundle bundle = objectMapper.readValue(inputStream, TemplateBundle.class);
      List<EventTemplate> importedTemplates = new ArrayList<>();

      for (TemplateBundle.TemplateExport templateExport : bundle.templates()) {
        EventTemplate template = toDomain(templateExport);
        template.setServer(serverService.ensureServer(serverId));
        importedTemplates.add(templateApplicationPort.save(template));
      }

      return importedTemplates;
    } catch (IOException e) {
      throw new IllegalStateException("Could not import templates", e);
    }
  }

  public TemplateBundle toBundle(Set<EventTemplate> templates) {
    return new TemplateBundle(templates.stream().map(this::toExport).toList());
  }

  private TemplateBundle.TemplateExport toExport(EventTemplate template) {
    return new TemplateBundle.TemplateExport(
        template.getKey(),
        template.getName(),
        template.getDetails(),
        template.getEmojiSlug(),
        template.getStrategies().stream().map(this::toExport).toList());
  }

  private TemplateBundle.StrategyExport toExport(EventStrategy strategy) {
    return new TemplateBundle.StrategyExport(
        strategy.getKey(),
        strategy.getName(),
        strategy.getGuides(),
        strategy.getPrimaryRoles().stream().map(this::toExport).toList(),
        strategy.getUtilityRoles().stream().map(this::toExport).toList());
  }

  private TemplateBundle.RoleExport toExport(EventRole role) {
    return new TemplateBundle.RoleExport(
        role.getKey(),
        role.getName(),
        role.getEmojiSlug(),
        role.getRequiredCount());
  }

  private TemplateBundle.UtilityRoleExport toExport(EventUtilityRole role) {
    return new TemplateBundle.UtilityRoleExport(
        role.getKey(),
        role.getName(),
        role.getEmojiSlug(),
        role.getRequiredCount(),
        new ArrayList<>(role.getEligiblePrimaryRoleKeys()));
  }

  private EventTemplate toDomain(TemplateBundle.TemplateExport templateExport) {
    EventTemplate template = new EventTemplate(
        templateExport.key(),
        templateExport.name(),
        templateExport.details(),
        templateExport.emojiSlug());

    for (TemplateBundle.StrategyExport strategyExport : templateExport.strategies()) {
      template.addStrategy(toDomain(strategyExport));
    }

    return template;
  }

  private EventStrategy toDomain(TemplateBundle.StrategyExport strategyExport) {
    EventStrategy strategy = new EventStrategy(strategyExport.key(), strategyExport.name());

    for (String guide : strategyExport.guides()) {
      strategy.addGuide(guide);
    }

    for (TemplateBundle.RoleExport roleExport : strategyExport.primaryRoles()) {
      strategy.addRole(new EventRole(
          roleExport.key(),
          roleExport.name(),
          roleExport.emojiSlug(),
          roleExport.requiredCount()));
    }

    for (TemplateBundle.UtilityRoleExport roleExport : strategyExport.utilityRoles()) {
      EventUtilityRole utilityRole = new EventUtilityRole(
          roleExport.key(),
          roleExport.name(),
          roleExport.emojiSlug(),
          roleExport.requiredCount());
      for (String eligiblePrimaryRoleKey : roleExport.eligiblePrimaryRoleKeys()) {
        utilityRole.addEligiblePrimaryRole(eligiblePrimaryRoleKey);
      }
      strategy.addRole(utilityRole);
    }

    return strategy;
  }
}
