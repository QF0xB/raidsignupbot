package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.listener;

import de.qf0xb.raidsignupbot.application.service.guild.GuildMemberSyncService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jspecify.annotations.NonNull;

@Slf4j
@ApplicationScoped
public class GuildRoleSyncListener extends ListenerAdapter {
  @Inject
  GuildMemberSyncService guildMemberSyncService;

  @Override
  public void onGuildMemberRoleAdd(@NonNull GuildMemberRoleAddEvent event) {
    event.getRoles().forEach(role -> {
      int synced = guildMemberSyncService.syncMemberToMatchingGuilds(
          event.getGuild().getId(),
          event.getMember().getId(),
          role.getId());

      if (synced > 0) {
        log.info(
            "Synced member {} into {} guild(s) after receiving role {} on server {}",
            event.getMember().getId(),
            synced,
            role.getId(),
            event.getGuild().getId());
      }
    });
  }
}
