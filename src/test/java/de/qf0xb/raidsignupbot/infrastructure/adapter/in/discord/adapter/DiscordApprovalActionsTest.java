package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.adapter;

import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.infrastructure.config.WebConfig;
import net.dv8tion.jda.api.components.buttons.Button;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscordApprovalActionsTest {
  private final DiscordEventUIApplicationAdapter adapter = new DiscordEventUIApplicationAdapter();
  private final Event event = Event.builder().id(7L).build();

  @Test
  void linksTheApprovalToTheWebUiWhenItIsConfigured() {
    adapter.webConfig = mock(WebConfig.class);
    when(adapter.webConfig.eventUrl(7L)).thenReturn(Optional.of("https://raids.example.com/events/7"));

    List<Button> buttons = adapter.approvalActions(event).getButtons();

    assertEquals(4, buttons.size());
    assertEquals("https://raids.example.com/events/7", buttons.getLast().getUrl());
  }

  @Test
  void keepsTheDiscordButtonsWithoutAWebUi() {
    adapter.webConfig = mock(WebConfig.class);
    when(adapter.webConfig.eventUrl(7L)).thenReturn(Optional.empty());

    assertEquals(List.of("event-approval:7:approve", "event-approval:7:reject", "event-approval:7:swap"),
        adapter.approvalActions(event).getButtons().stream().map(Button::getCustomId).toList());
  }
}
