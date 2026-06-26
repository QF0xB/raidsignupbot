package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.server;

import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.RaidCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@ApplicationScoped
public class ServerPermissionsCommand extends RaidCommand {
  private static final String OPTION_GUILD_ROLE = "guild_role";
  private static final String OPTION_TEMPLATE_ROLE = "template_role";
  private static final String OPTION_EVENT_ROLE = "event_role";
  private static final String OPTION_MANAGE_SIGNUP_CHANNELS = "manage_signup_channels";
  private static final String OPTION_MANAGE_VOICE_CHANNELS = "manage_voice_channels";
  private static final String OPTION_RAID_VOICE_CHANNELS_CATEGORY = "raid_voice_channels_category";

  @Inject
  ServerService serverService;

  public ServerPermissionsCommand() {
    super("setserverpermissions", "Configure server-scoped permission roles.", CommandAccessLevel.MANAGE_GUILDS);
    addOption(new OptionData(OptionType.ROLE, OPTION_GUILD_ROLE, "Role that may manage guild configuration", false));
    addOption(new OptionData(OptionType.ROLE, OPTION_TEMPLATE_ROLE, "Role that may manage templates", false));
    addOption(new OptionData(OptionType.ROLE, OPTION_EVENT_ROLE, "Role that may manage events", false));
    addOption(new OptionData(OptionType.BOOLEAN, OPTION_MANAGE_SIGNUP_CHANNELS, "Whether events should create a signupchannel", false));
    addOption(new OptionData(OptionType.BOOLEAN, OPTION_MANAGE_VOICE_CHANNELS, "Whether events should create voicechannels", false));
    addOption(new OptionData(OptionType.STRING, OPTION_RAID_VOICE_CHANNELS_CATEGORY, "Which Category has the Raid VCs? This will try a best effort.", false));
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    if (event.getGuild() == null) {
      event.reply("This command can only be used in a server.").setEphemeral(true).queue();
      return;
    }

    String serverId = event.getGuild().getId();

    Role guildRole = event.getOption(OPTION_GUILD_ROLE) == null ? null : event.getOption(OPTION_GUILD_ROLE).getAsRole();
    Role templateRole = event.getOption(OPTION_TEMPLATE_ROLE) == null ? null : event.getOption(OPTION_TEMPLATE_ROLE).getAsRole();
    Role eventRole = event.getOption(OPTION_EVENT_ROLE) == null ? null : event.getOption(OPTION_EVENT_ROLE).getAsRole();
    Boolean manageSignupChannels = event.getOption(OPTION_MANAGE_SIGNUP_CHANNELS) == null ? null : event.getOption(OPTION_MANAGE_SIGNUP_CHANNELS).getAsBoolean();
    Boolean manageVoiceChannels = event.getOption(OPTION_MANAGE_VOICE_CHANNELS) == null ? null : event.getOption(OPTION_MANAGE_VOICE_CHANNELS).getAsBoolean();
    String raidVoiceChannelCategory = event.getOption(OPTION_RAID_VOICE_CHANNELS_CATEGORY) == null ? null : event.getOption(OPTION_RAID_VOICE_CHANNELS_CATEGORY).getAsString();

    if (guildRole == null && templateRole == null && eventRole == null && manageSignupChannels == null && manageVoiceChannels == null && raidVoiceChannelCategory == null) {
      Server server = serverService.findServerByIdOptional(serverId)
          .orElseGet(() -> serverService.ensureServer(serverId));
      event.reply(formatCurrent(server)).setEphemeral(true).queue();
      return;
    }

    Server server = serverService.updateSettings(
        serverId,
        roleId(guildRole),
        roleId(templateRole),
        roleId(eventRole),
        manageSignupChannels,
        manageVoiceChannels,
        raidVoiceChannelCategory);

    event.reply(formatCurrent(server)).setEphemeral(true).queue();
  }

  @Override
  public void autoComplete(CommandAutoCompleteInteractionEvent event) {
    // Role options are handled by Discord.
  }

  private String roleId(Role role) {
    return role == null ? null : role.getId();
  }

  private String formatCurrent(Server server) {

    String channelsMessage;
    if (server.isManageVoiceChannels() && server.isManageSignupChannels()) {
      channelsMessage = "voice + signup channels";
    } else if (server.isManageVoiceChannels()) {
      channelsMessage = "voice channels only";
    } else if (server.isManageSignupChannels()) {
      channelsMessage = "signup channels only";
    } else {
      channelsMessage = "none";
    }

    return "Server permissions updated:\n"
        + "- Guilds: " + valueOrNone(server.getGuildManagerRoleId()) + "\n"
        + "- Templates: " + valueOrNone(server.getTemplateManagerRoleId()) + "\n"
        + "- Events: " + valueOrNone(server.getEventManagerRoleId()) + "\n"
        + "- Managing channels: " + channelsMessage;
  }

  private String valueOrNone(String value) {
    return value == null || value.isBlank() ? "none" : "<@&" + value + ">";
  }
}
