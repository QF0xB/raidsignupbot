package de.qf0xb.raidsignupbot.domain.model.template;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Locale;

@Getter
@Setter
@EqualsAndHashCode(of = "key")
@ToString
public class EventRole {
  private final String key;
  private String name;
  private String emojiSlug;
  private int requiredCount;

  public EventRole(String key, String name, String emojiSlug, int requiredCount) {
    this.key = key;
    this.name = name;
    setEmojiSlug(emojiSlug);
    this.requiredCount = requiredCount;
  }

  public void setEmojiSlug(String emojiSlug) {
    this.emojiSlug = normalizeEmojiSlug(emojiSlug);
  }

  public static String normalizeEmojiSlug(String emojiSlug) {
    if (emojiSlug == null) {
      return null;
    }

    String trimmed = emojiSlug.trim();
    String normalized = trimmed.toLowerCase(Locale.ROOT);

    return switch (normalized) {
      case ":0:", ":zero:" -> "0️⃣";
      case ":1:", ":one:" -> "1️⃣";
      case ":2:", ":two:" -> "2️⃣";
      case ":3:", ":three:" -> "3️⃣";
      case ":4:", ":four:" -> "4️⃣";
      case ":5:", ":five:" -> "5️⃣";
      case ":6:", ":six:" -> "6️⃣";
      case ":7:", ":seven:" -> "7️⃣";
      case ":8:", ":eight:" -> "8️⃣";
      case ":9:", ":nine:" -> "9️⃣";
      default -> normalizeLetterAlias(trimmed, normalized);
    };
  }

  private static String normalizeLetterAlias(String originalEmojiSlug, String loweredEmojiSlug) {
    if (loweredEmojiSlug.startsWith(":") && loweredEmojiSlug.endsWith(":")) {
      String alias = loweredEmojiSlug.substring(1, loweredEmojiSlug.length() - 1);
      if (alias.length() == 1 && alias.charAt(0) >= 'a' && alias.charAt(0) <= 'z') {
        return toRegionalIndicator(alias.charAt(0));
      }

      String prefix = "regional_indicator_";
      if (alias.startsWith(prefix) && alias.length() == prefix.length() + 1) {
        char letter = alias.charAt(prefix.length());
        if (letter >= 'a' && letter <= 'z') {
          return toRegionalIndicator(letter);
        }
      }
    }

    return originalEmojiSlug;
  }

  private static String toRegionalIndicator(char letter) {
    return new String(Character.toChars(0x1F1E6 + (letter - 'a')));
  }
}
