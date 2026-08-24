# onyxguildbot

## Raid calendar

Guild administrators can configure the calendar with `/calendarconfig`, providing a text channel. These commands use the bot's modal workflow: `/calendarconfig` sets the channel, `/scheduleevent` schedules an entry, and `/calendarremove` removes an unused entry. `/calendar` refreshes the configured channel's calendar embed.

The scheduled event hoster creates signup events through `/createevent`. Guild managers (and server administrators) can create any available calendar event as an override. The wizard only offers eligible unused entries, and event creation is rejected unless the selected event still matches that entry. This keeps the calendar channel as the source of truth for upcoming events.

Each guild can have at most one raid on a calendar day. The check includes both calendar entries and existing persisted raid events, including events created before the calendar was introduced.

This project uses Quarkus, the Supersonic Subatomic Java Framework.

Every event signup message includes a 🛟 Backup reaction. Members should select their normal roles plus Backup when applicable; Backup applies a `-100` priority penalty without excluding them from normal role assignment. After teams are generated, players without a team receive a private message asking whether they are available as a backup for each selected position. Only one backup can accept each position.

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
- JDBC Driver - MariaDB ([guide](https://quarkus.io/guides/datasource)): Connect to the MariaDB database via JDBC
- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): Simplified JPA/Hibernate data access layer with active record and repository patterns
- Scheduler ([guide](https://quarkus.io/guides/scheduler)): Schedule recurring tasks and periodic jobs using cron expressions or fixed intervals
