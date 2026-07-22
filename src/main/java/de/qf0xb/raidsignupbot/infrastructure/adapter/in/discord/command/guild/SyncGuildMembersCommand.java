package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.guild;

import de.qf0xb.raidsignupbot.application.service.guild.GuildMemberSyncService;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.RaidCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@ApplicationScoped
public class SyncGuildMembersCommand extends RaidCommand {
  @Inject
  GuildMemberSyncService guildMemberSyncService;

  public SyncGuildMembersCommand() {
    super("syncguildmembers", "Synchronize guild members from Discord roles.", CommandAccessLevel.MANAGE_GUILDS);
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    if (event.getGuild() == null) {
      event.reply("This command can only be used in a guild.")
          .setEphemeral(true)
          .queue();
      return;
    }

    int synced = guildMemberSyncService.syncGuildMembersByServerId(event.getGuild().getId());
    event.reply("Synced " + synced + " guild member(s) for this server.")
        .setEphemeral(true)
        .queue();
  }

  @Override
  public void autoComplete(CommandAutoCompleteInteractionEvent event) {
    // No autocomplete options.
  }
}
