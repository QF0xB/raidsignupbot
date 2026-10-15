package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command;

public enum CommandAccessLevel {
  PUBLIC,
  MANAGE_GUILDS,
  MANAGE_TEMPLATES,
  MANAGE_EVENTS,
  /** Server administrators, managers, and the server owner only; no bot role grants it. */
  ADMIN
}
