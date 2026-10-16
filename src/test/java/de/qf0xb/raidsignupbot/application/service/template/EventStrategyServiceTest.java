package de.qf0xb.raidsignupbot.application.service.template;

import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventStrategyServiceTest {
  private final EventTemplateApplicationPort port = mock(EventTemplateApplicationPort.class);
  private final TemplateService templateService = new TemplateService(port, mock(ServerService.class));
  private final EventStrategyService service = new EventStrategyService(templateService);
  private final EventTemplate template = new EventTemplate("raid", "Raid", "Details", "dragon");
  private final EventStrategy strategy = new EventStrategy("main", "Main");

  @BeforeEach
  void setUp() {
    strategy.addGuide("old guide");
    strategy.addRole(new EventRole("fire", "Fire", "🔥", 1));
    strategy.addRole(new EventRole("ice", "Ice", "❄️", 1));
    EventUtilityRole utility = new EventUtilityRole("blade", "Blade", "🗡️", 1);
    utility.addEligiblePrimaryRole("fire");
    strategy.addRole(utility);
    template.addStrategy(strategy);
    when(port.findByServerIdAndKey("server", "raid")).thenReturn(Optional.of(template));
    when(port.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void replacesTheGuidesInsteadOfAppendingThem() {
    service.updateStrategy("server", "raid", "main", "Renamed", List.of("new guide"));

    assertEquals("Renamed", strategy.getName());
    assertEquals(List.of("new guide"), strategy.getGuides());
  }

  @Test
  void updatesRolesAndTheEligiblePrimaryRolesOfUtilities() {
    service.updateRole("server", "raid", "main", "fire", "Flame", "🔥", 2, Set.of("ignored"));
    service.updateRole("server", "raid", "main", "blade", "Blade", "🗡️", 2, Set.of("ice"));

    assertEquals("Flame", strategy.findRole("fire").orElseThrow().getName());
    assertEquals(2, strategy.findRole("fire").orElseThrow().getRequiredCount());
    assertEquals(Set.of("ice"), strategy.findUtilityRole("blade").orElseThrow().getEligiblePrimaryRoleKeys());
  }

  @Test
  void updatesTemplateDetails() {
    templateService.updateTemplate("server", "raid", "Big Raid", "crown", "New details");

    assertEquals("Big Raid", template.getName());
    assertEquals("crown", template.getEmojiSlug());
    assertEquals("New details", template.getDetails());
  }

  @Test
  void rejectsUnknownRoles() {
    assertThrows(IllegalArgumentException.class,
        () -> service.updateRole("server", "raid", "main", "missing", "Name", "🔥", 1, Set.of()));
  }
}
