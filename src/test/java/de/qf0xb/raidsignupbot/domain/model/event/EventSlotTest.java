package de.qf0xb.raidsignupbot.domain.model.event;

import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventSlotTest {
  @Test
  void utilityRolesAreNotPrimarySlots() {
    EventUtilityRole utilityRole = new EventUtilityRole("hex", "Hex Maycast", "schoolSun", 1);

    EventRole role = utilityRole; // Obfuscate for better checking

    assertThrows(IllegalArgumentException.class, () -> new EventSlot(role));
  }

  @Test
  void testPrimaryCopyConstructor() {
    EventRole role = new EventRole("tank", "Tank", "tank", 1);
    EventSlot slot = new EventSlot(role);

    assertFalse(slot.isUtility());
    assertEquals("tank", slot.getKey());
    assertEquals("Tank", slot.getRoleName());
    assertEquals("tank", slot.getEmojiSlug());

    assertTrue(slot.matches("tank"));
  }
}
