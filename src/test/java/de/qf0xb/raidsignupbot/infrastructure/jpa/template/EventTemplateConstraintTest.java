package de.qf0xb.raidsignupbot.infrastructure.jpa.template;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.hibernate.exception.ConstraintViolationException;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class EventTemplateConstraintTest {
  @Inject
  EntityManager entityManager;

  @Test
  @TestTransaction
  void allowsSameStrategyKeyInDifferentTemplates() {
    persistTemplateWithStrategy("template-a", "shared-strategy", "Storm A");
    persistTemplateWithStrategy("template-b", "shared-strategy", "Storm B");

    assertDoesNotThrow(() -> entityManager.flush());
  }

  @Test
  @TestTransaction
  void allowsSameRoleKeyInDifferentStrategies() {
    EventTemplateEntity template = new EventTemplateEntity();
    template.setKey("template-a");
    template.setName("Template A");
    template.setDetails("details");

    EventStrategyEntity firstStrategy = new EventStrategyEntity();
    firstStrategy.setKey("first-strategy");
    firstStrategy.setName("Storm");
    firstStrategy.setEventTemplateEntity(template);
    template.getEventStrategies().add(firstStrategy);

    EventStrategyEntity secondStrategy = new EventStrategyEntity();
    secondStrategy.setKey("second-strategy");
    secondStrategy.setName("Myth");
    secondStrategy.setEventTemplateEntity(template);
    template.getEventStrategies().add(secondStrategy);

    firstStrategy.getEventRoleEntities().add(primaryRole("one", firstStrategy));
    secondStrategy.getEventRoleEntities().add(primaryRole("one", secondStrategy));

    entityManager.persist(template);

    assertDoesNotThrow(() -> entityManager.flush());
  }

  @Test
  @TestTransaction
  void rejectsDuplicateStrategyKeyInSameTemplate() {
    EventTemplateEntity template = new EventTemplateEntity();
    template.setKey("template-a");
    template.setName("Template A");
    template.setDetails("details");

    EventStrategyEntity firstStrategy = new EventStrategyEntity();
    firstStrategy.setKey("storm");
    firstStrategy.setName("Storm");
    firstStrategy.setEventTemplateEntity(template);
    template.getEventStrategies().add(firstStrategy);

    EventStrategyEntity secondStrategy = new EventStrategyEntity();
    secondStrategy.setKey("storm");
    secondStrategy.setName("Storm Copy");
    secondStrategy.setEventTemplateEntity(template);
    template.getEventStrategies().add(secondStrategy);

    assertThrows(ConstraintViolationException.class, () -> entityManager.persist(template));
  }

  @Test
  @TestTransaction
  void rejectsDuplicateRoleKeyInSameStrategy() {
    EventTemplateEntity template = new EventTemplateEntity();
    template.setKey("template-a");
    template.setName("Template A");
    template.setDetails("details");

    EventStrategyEntity strategy = new EventStrategyEntity();
    strategy.setKey("role-strategy");
    strategy.setName("Storm");
    strategy.setEventTemplateEntity(template);
    template.getEventStrategies().add(strategy);

    strategy.getEventRoleEntities().add(primaryRole("one", strategy));
    strategy.getEventRoleEntities().add(primaryRole("one", strategy));

    assertThrows(ConstraintViolationException.class, () -> entityManager.persist(template));
  }

  private void persistTemplateWithStrategy(String templateKey, String strategyKey, String strategyName) {
    EventTemplateEntity template = new EventTemplateEntity();
    template.setKey(templateKey);
    template.setName(templateKey);
    template.setDetails("details");

    EventStrategyEntity strategy = new EventStrategyEntity();
    strategy.setKey(strategyKey);
    strategy.setName(strategyName);
    strategy.setEventTemplateEntity(template);
    template.getEventStrategies().add(strategy);

    entityManager.persist(template);
  }

  private EventRoleEntity primaryRole(String roleKey, EventStrategyEntity strategy) {
    EventRoleEntity role = new EventRoleEntity();
    role.setKey(roleKey);
    role.setName("Role " + roleKey);
    role.setEmojiSlug(":one:");
    role.setRequiredCount(1);
    role.setEventStrategyEntity(strategy);
    return role;
  }
}
