package de.qf0xb.raidsignupbot.application.dto.template;

import java.util.List;

public record TemplateBundle(List<TemplateExport> templates) {
  public record TemplateExport(
      String key,
      String name,
      String details,
      String emojiSlug,
      List<StrategyExport> strategies) {}

  public record StrategyExport(
      String key,
      String name,
      List<String> guides,
      List<RoleExport> primaryRoles,
      List<UtilityRoleExport> utilityRoles) {}

  public record RoleExport(
      String key,
      String name,
      String emojiSlug,
      int requiredCount) {}

  public record UtilityRoleExport(
      String key,
      String name,
      String emojiSlug,
      int requiredCount,
      List<String> eligiblePrimaryRoleKeys) {}
}
