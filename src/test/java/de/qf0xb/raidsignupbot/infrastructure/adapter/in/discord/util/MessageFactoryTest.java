package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.util;

import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageFactoryTest {
  @Test
  void displaysSlotIdsAndShowsWhichBoxHasEachUtility() {
    EventStrategy strategy = new EventStrategy("main", "Main");
    strategy.addRole(new EventRole("fire", "Fire", "🔥", 1));
    strategy.addRole(new EventRole("support", "Support", "☀️", 1));
    EventUtilityRole utility = new EventUtilityRole("hex", "Hex", "☀️", 1);
    utility.addEligiblePrimaryRole("support");
    strategy.addRole(utility);
    EventTeam team = new EventTeam(strategy);
    GuildMember dual = new GuildMember("dual");
    team.assign("fire", dual);
    team.assign("support", dual);
    team.assignUtility("hex", dual);

    String text = new MessageFactory().buildTeam(team, 2);

    assertTrue(text.contains("`2:1` 🔥 (Fire): <@dual>"), text);
    assertTrue(text.contains("`2:2` ☀️ (Support): <@dual>"), text);
    assertTrue(text.contains("↳ `2:2` ☀️ (Hex): <@dual>"), text);
  }

  @Test
  void buildsSignupEmbedWithRolesInTemplateOrder() {
    DiscordEmojiResolver emojiResolver = mock(DiscordEmojiResolver.class);
    when(emojiResolver.resolve(anyString())).thenAnswer(invocation -> {
      String slug = invocation.getArgument(0);
      if (slug.equals(":nullity:")) {
        throw new IllegalStateException("Unsupported emoji slug: " + slug);
      }
      return Emoji.fromUnicode(EventRole.normalizeEmojiSlug(slug).startsWith(":") ? "🌞" : EventRole.normalizeEmojiSlug(slug));
    });
    MessageFactory factory = new MessageFactory();
    factory.emojiResolver = emojiResolver;

    EventStrategy strategy = new EventStrategy("full-raid", "Full Raid");
    strategy.addRole(new EventRole("vanguard-fire-1", "Vanguard - Fire 1", ":regional_indicator_a:", 1));
    strategy.addRole(new EventRole("vanguard-fire-2", "Vanguard - Fire 2", ":regional_indicator_b:", 1));
    strategy.addRole(new EventRole("oct-surge-support", "OCT - Surge Support", ":one:", 1));
    EventUtilityRole hex = new EventUtilityRole("hex", "WC Hex", ":schoolSun:", 1);
    hex.addEligiblePrimaryRole("vanguard-fire-1");
    strategy.addRole(hex);
    strategy.addGuide("Vanguard Guide | https://example.com/vanguard");
    strategy.addGuide("https://example.com/outside");
    EventTemplate template = new EventTemplate("void", "Voracious Void Raid", "Bring your gear.", "nullity");
    template.addStrategy(strategy);

    Event event = Event.builder()
        .guild(new Guild("Onyx Cats", "server-1"))
        .hoster(new GuildMember("hoster-1"))
        .template(template)
        .donation(20)
        .start(Instant.parse("2026-10-16T17:00:00Z"))
        .end(Instant.parse("2026-10-16T20:00:00Z"))
        .build();

    MessageEmbed embed = factory.buildSignupEmbed(event).build();

    assertEquals("Voracious Void Raid", embed.getTitle());
    assertEquals("Bring your gear.", embed.getDescription());
    List<String> fieldNames = embed.getFields().stream().map(MessageEmbed.Field::getName).toList();
    assertEquals(
        List.of("🗓️ Date", "⏱️ Duration", "👑 Host", "🏰 Guild", "💰 Donations", "‎",
            "⚔️ Roles", "📖 Guides", "🛟 Backup", "📦 Dual Box", "📋 How to join", "📌 Rules"),
        fieldNames);
    String roles = field(embed, "⚔️ Roles");
    assertEquals(
        "🇦  Vanguard - Fire 1\n🇧  Vanguard - Fire 2\n1️⃣  OCT - Surge Support\n🌞  WC Hex · *utility, by 🇦*",
        roles);
    assertEquals("[Vanguard Guide](https://example.com/vanguard)\n[Guide 2](https://example.com/outside)",
        field(embed, "📖 Guides"));
    assertEquals("20 Azoth", field(embed, "💰 Donations"));
    assertTrue(field(embed, "🗓️ Date").contains("<t:1792170000:R>"));
    assertTrue(field(embed, "📦 Dual Box").contains("two primary slots"));
  }

  private String field(MessageEmbed embed, String name) {
    return embed.getFields().stream()
        .filter(field -> name.equals(field.getName()))
        .findFirst()
        .orElseThrow()
        .getValue();
  }
}
