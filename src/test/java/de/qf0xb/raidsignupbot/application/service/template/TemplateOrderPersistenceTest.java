package de.qf0xb.raidsignupbot.application.service.template;

import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class TemplateOrderPersistenceTest {
  private static final List<String> ROLE_KEYS = IntStream.rangeClosed(1, 12).mapToObj(i -> "role-" + i).toList();

  @Inject
  TemplateImportExportService importExportService;

  @Inject
  EventTemplateApplicationPort templatePort;

  @Test
  void keepsStrategyRoleAndGuideOrderAfterReload() {
    String roles = ROLE_KEYS.stream()
        .map(key -> String.format(
            "{\"key\":\"%s\",\"name\":\"%s\",\"emojiSlug\":\":one:\",\"requiredCount\":1}", key, key))
        .reduce((left, right) -> left + "," + right)
        .orElseThrow();
    String json = """
        {"templates":[{"key":"order-test","name":"Order Test","details":"","emojiSlug":"coin","strategies":[
          {"key":"zeta","name":"Zeta","guides":["Z guide | https://z.example","A guide | https://a.example"],
           "primaryRoles":[%s],"utilityRoles":[]},
          {"key":"alpha","name":"Alpha","guides":[],"primaryRoles":[%s],"utilityRoles":[]}
        ]}]}
        """.formatted(roles, roles);

    importExportService.importTemplates("order-server", new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

    EventTemplate loaded = templatePort.findByServerIdAndKey("order-server", "order-test").orElseThrow();
    assertEquals(List.of("zeta", "alpha"), loaded.getStrategies().stream().map(EventStrategy::getKey).toList());
    EventStrategy zeta = loaded.getStrategies().iterator().next();
    assertEquals(ROLE_KEYS, zeta.getPrimaryRoles().stream().map(EventRole::getKey).toList());
    assertEquals(List.of("Z guide | https://z.example", "A guide | https://a.example"), zeta.getGuides());
  }
}
