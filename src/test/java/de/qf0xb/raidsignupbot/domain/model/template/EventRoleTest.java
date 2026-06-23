package de.qf0xb.raidsignupbot.domain.model.template;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventRoleTest {
  @Test
  void equalityUsesOnlyKey() {
    EventRole first = new EventRole("tank", "Tank", "shield", 1);
    EventRole second = new EventRole("tank", "Main Tank", "bigShield", 3);
    EventRole other = new EventRole("healer", "Healer", "plus", 1);

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
    assertNotEquals(first, other);
  }

  @Test
  void settersOnlyChangePresentationFields() {
    EventRole role = new EventRole("tank", "Tank", "shield", 1);

    role.setName("Main Tank");
    role.setEmojiSlug("bigShield");
    role.setRequiredCount(2);

    assertEquals("tank", role.getKey());
    assertEquals("Main Tank", role.getName());
    assertEquals("bigShield", role.getEmojiSlug());
    assertEquals(2, role.getRequiredCount());
    assertTrue(role.toString().contains("tank"));
  }

  @Test
  void normalizesCommonDiscordEmojiAliases() {
    EventRole numeric = new EventRole("one", "One", ":1:", 1);
    EventRole letter = new EventRole("a", "A", ":a:", 1);
    EventRole letterAlias = new EventRole("z", "Z", ":regional_indicator_z:", 1);
    EventRole custom = new EventRole("hex", "Hex", "<:hex:123456789012345678>", 1);

    assertEquals("1️⃣", numeric.getEmojiSlug());
    assertEquals("🇦", letter.getEmojiSlug());
    assertEquals("🇿", letterAlias.getEmojiSlug());
    assertEquals("<:hex:123456789012345678>", custom.getEmojiSlug());
  }

  @Test
  void normalizesAllLetterAliasesAThroughZ() {
    for (char letter = 'a'; letter <= 'z'; letter++) {
      EventRole role = new EventRole(String.valueOf(letter), String.valueOf(letter).toUpperCase(), ":" + letter + ":", 1);

      assertEquals(
          new String(Character.toChars(0x1F1E6 + (letter - 'a'))),
          role.getEmojiSlug());
    }
  }
}
