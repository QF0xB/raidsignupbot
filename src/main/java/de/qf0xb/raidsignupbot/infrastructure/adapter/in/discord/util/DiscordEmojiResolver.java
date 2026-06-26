package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.util;

import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.DiscordBot;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;

import java.util.List;

@ApplicationScoped
public class DiscordEmojiResolver {
  @Inject
  JDA api;

  public Emoji resolve(String emojiSlug) {
    String normalized = EventRole.normalizeEmojiSlug(emojiSlug);

    if (normalized != null && normalized.startsWith(":") && normalized.endsWith(":")) {
      String customEmojiName = normalized.substring(1, normalized.length() - 1);
      List<RichCustomEmoji> customEmojis = api.getEmojisByName(customEmojiName, true);
      if (!customEmojis.isEmpty()) {
        return customEmojis.getFirst();
      }

      throw new IllegalStateException("Unsupported emoji slug: " + emojiSlug);
    }

    assert normalized != null;
    return Emoji.fromFormatted(normalized);
  }
}
