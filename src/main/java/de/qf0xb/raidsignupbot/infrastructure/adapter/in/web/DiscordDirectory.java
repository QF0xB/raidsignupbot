package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import de.qf0xb.raidsignupbot.domain.model.guild.SimulatedPlayer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/** Roles, channels, and members of a Discord server for the web forms' select boxes. */
@ApplicationScoped
public class DiscordDirectory {
  private final JDA api;

  public DiscordDirectory(JDA api) {
    this.api = api;
  }

  public record Option(String id, String name) {}

  public List<Option> roles(String serverId) {
    return server(serverId).getRoles().stream()
        .filter(role -> !role.isPublicRole() && !role.isManaged())
        .map(role -> new Option(role.getId(), role.getName()))
        .toList();
  }

  public List<Option> textChannels(String serverId) {
    return options(server(serverId).getTextChannels(), channel -> new Option(channel.getId(), "#" + channel.getName()));
  }

  public List<Option> voiceChannels(String serverId) {
    return options(server(serverId).getVoiceChannels(), channel -> new Option(channel.getId(), channel.getName()));
  }

  public List<Option> categories(String serverId) {
    return options(server(serverId).getCategories(), category -> new Option(category.getId(), category.getName()));
  }

  public List<Option> members(String serverId) {
    return server(serverId).getMembers().stream()
        .filter(member -> !member.getUser().isBot())
        .map(member -> new Option(member.getId(), member.getEffectiveName()))
        .sorted(Comparator.comparing(Option::name, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  public String memberName(String serverId, String userId) {
    if (userId == null) {
      return "—";
    }
    if (SimulatedPlayer.isSimulated(userId)) {
      return SimulatedPlayer.displayName(userId);
    }
    Guild server = api.getGuildById(serverId);
    Member member = server == null ? null : server.getMemberById(userId);
    return member == null ? userId : member.getEffectiveName();
  }

  public String channelName(String channelId) {
    if (channelId == null) {
      return "—";
    }
    var channel = api.getGuildChannelById(channelId);
    return channel == null ? channelId : channel.getName();
  }

  public String roleName(String roleId) {
    if (roleId == null) {
      return "—";
    }
    var role = api.getRoleById(roleId);
    return role == null ? roleId : role.getName();
  }

  private Guild server(String serverId) {
    Guild server = api.getGuildById(serverId);
    if (server == null) {
      throw new NotFoundException("The bot is not on this server.");
    }
    return server;
  }

  private static <T> List<Option> options(List<T> channels, Function<T, Option> mapper) {
    return channels.stream().map(mapper).toList();
  }
}
