package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.util;

import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.DiscordBot;
import jakarta.enterprise.context.ApplicationScoped;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;

import java.util.List;

@ApplicationScoped
public class DiscordEmojiResolver {
  private final DiscordBot bot;

  public DiscordEmojiResolver(DiscordBot bot) {
    this.bot = bot;
  }

  public Emoji resolve(String emojiSlug) {
    String normalized = EventRole.normalizeEmojiSlug(emojiSlug);

    if (normalized != null && normalized.startsWith(":") && normalized.endsWith(":")) {
      String customEmojiName = normalized.substring(1, normalized.length() - 1);
      List<RichCustomEmoji> customEmojis = bot.getApi().getEmojisByName(customEmojiName, true);
      if (!customEmojis.isEmpty()) {
        return customEmojis.getFirst();
      }

      throw new IllegalStateException("Unsupported emoji slug: " + emojiSlug);
    }

    return Emoji.fromFormatted(normalized);
  }
}
