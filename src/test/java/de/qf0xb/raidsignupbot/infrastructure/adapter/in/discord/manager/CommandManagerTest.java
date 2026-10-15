package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.manager;

import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandPermissionService;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.SlashCommandDefinition;
import jakarta.enterprise.inject.Instance;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommandManagerTest {
  @Test
  void rejectsCommandsFromServersTheBotIsNotIn() {
    CommandPermissionService permissionService = mock(CommandPermissionService.class);
    SlashCommandDefinition command = mock(SlashCommandDefinition.class);
    SlashCommandInteractionEvent interaction = mock(SlashCommandInteractionEvent.class, RETURNS_DEEP_STUBS);
    Guild detachedGuild = mock(Guild.class);

    when(command.hasName("onboard")).thenReturn(true);
    when(command.getAccessLevel()).thenReturn(CommandAccessLevel.MANAGE_GUILDS);
    when(detachedGuild.isDetached()).thenReturn(true);
    when(interaction.getName()).thenReturn("onboard");
    when(interaction.getGuild()).thenReturn(detachedGuild);

    @SuppressWarnings("unchecked")
    CommandManager manager = new CommandManager(mock(JDA.class), mock(Instance.class), permissionService);
    manager.addCommand(command);

    manager.onSlashCommandInteraction(interaction);

    verify(interaction).reply(contains("not a member of this server"));
    verify(permissionService, never()).isAllowed(any(), any());
    verify(command, never()).execute(any());
  }
}
