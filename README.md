# onyxguildbot

## Raid calendar

Guild administrators can configure the calendar with `/calendarconfig`, providing a text channel. These commands use the bot's modal workflow: `/calendarconfig` sets the channel, `/scheduleevent` schedules an entry, and `/calendarremove` removes an unused entry. `/calendar` refreshes the configured channel's calendar embed.

The scheduled event hoster creates signup events through `/createevent`. Guild managers (and server administrators) can create any available calendar event as an override. The wizard only offers eligible unused entries, and event creation is rejected unless the selected event still matches that entry. This keeps the calendar channel as the source of truth for upcoming events.

Each guild can have at most one raid on a calendar day. The check includes both calendar entries and existing persisted raid events, including events created before the calendar was introduced.

This project uses Quarkus, the Supersonic Subatomic Java Framework.

## Web UI

The bot serves a web UI on port 8080 that offers everything the slash commands do; the commands keep working, and both use the same services, so every change shows up in Discord. Users log in with Discord, and each page requires the same permission as its command (server administrators, Manage Server, the owner, or the configured manager roles):

- **Events** (event managers): edit details, close signup, remove signups, swap boxes using the `team:slot` numbering, postpone, close, and delete (admins only).
- **Approval** (the event admin): approve, reject, or swap boxes in the proposal. Approval messages in Discord link to this page when `WEB_BASE_URL` is set.
- **Calendar**: view and refresh it; guild managers schedule and remove entries and choose the calendar channel. Guild managers and the entry's host create events from free entries.
- **Templates** (template managers): create, edit details, strategies, guides, and roles, import, and export.
- **Guilds & settings** (guild managers): manager roles, channel handling, guilds, and member sync.

Setup:

1. In the Discord developer portal, open the bot's application, copy the OAuth2 client secret into `DISCORD_CLIENT_SECRET`, and add `https://<WEB_HOST>/auth/callback` as a redirect.
2. Set `WEB_HOST` in `.env`. `compose.yaml` labels the bot for Traefik, which terminates TLS. The app trusts `X-Forwarded-*` headers, so don't publish port 8080 directly.

In dev mode, add `http://localhost:8080/auth/callback` as a redirect and export `DISCORD_CLIENT_SECRET`.

## Postponing a raid

Event managers can use `/postponeraid event:<raid> minutes:30` to delay an upcoming raid. Both start and end move by the given number of minutes, preserving the duration, signup reactions, teams, pending approval, and backup responses. The bot updates the signup and published teams messages, moves the linked calendar entry when needed, refreshes the calendar, and posts the new start time in the signup channel. Reminders follow the new schedule without immediately repeating ones already sent. The command rejects raids that have started and moves that would create a second raid for the same guild on the same day.

## Dual-box signups

React with **📦 Dual Box** in addition to your normal role reactions to volunteer to play two characters. The team generator may assign you to **up to two primary slots across the entire raid**. It prefers keeping both boxes on the same team, but can split them when needed to fill teams or respect role requirements. Only your selected roles are eligible. Without 📦, the limit remains one primary slot. The generator prefers seating another player over using a second slot when the number of teams is unchanged. Dual Box can be combined with 🛟 Backup; the backup priority penalty still applies. Removing 📦 before team generation restores the one-slot limit.

### Switching players and individual boxes

`/swapteam` exchanges **one primary slot per selected player**, rather than replacing every occurrence of either player. Teams messages label each primary slot with `team:slot`, for example `1:2` means team 1, primary slot 2. Utility lines show the slot they belong to. If a player has multiple boxes, specify `left_slot` or `right_slot`; otherwise the bot asks you to choose and leaves the teams unchanged. For single-slot players the slot options can be omitted.

For example, `/swapteam event:<raid> left_member:@Alice right_member:@Bob left_slot:1:2` swaps only Alice's box in slot `1:2` with Bob's single box. Alice's other box stays where it is. The selected positions' utility duties transfer to the incoming player. If one selected player is not assigned, they replace only the specified box.

In the approval message, **Swap members** opens two selectors containing the assigned boxes, including a separate entry for each dual-box slot. Choose one box for each player. If the roster has more than 25 slots, enter the two `team:slot` IDs from the latest proposal instead. The form rejects stale or invalid choices and is restricted to the event admin. After a successful swap, the bot sends a refreshed approval proposal and removes the old message's controls. Approval swaps only exchange assigned players; they do not publish the teams.

## Backup signups

Every event signup message includes a 🛟 Backup reaction. Members should select their normal roles plus Backup when applicable; Backup applies a `-100` priority penalty without excluding them from normal role assignment. After teams are published, players without a team receive one private backup request per event listing all their eligible roles. Accepting confirms them for the listed positions that still need a backup; declining applies to the whole request. Only one backup can accept each position. The player receives one combined confirmation, and each organizer receives one combined notification.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following raidCommand:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Building the Docker image locally

The JVM image only copies the build output, so both architectures build without emulation. With Docker's containerd image store enabled, the multi-arch image can be loaded locally:

```shell script
mvn package
docker buildx build --platform linux/amd64,linux/arm64 -f src/main/docker/Dockerfile.jvm \
  -t ghcr.io/qf0xb/raidsignupbot:local --load .
```

Replace `--load` with `--push` to publish the manifest list instead. Run it with `BOT_TAG=local docker compose up -d`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/onyxguildbot-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- Hibernate Validator ([guide](https://quarkus.io/guides/validation)): Bean validation using Hibernate Validator and Jakarta Validation annotations
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC
- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): Simplified JPA/Hibernate data access layer with active record and repository patterns
- Scheduler ([guide](https://quarkus.io/guides/scheduler)): Schedule recurring tasks and periodic jobs using cron expressions or fixed intervals
