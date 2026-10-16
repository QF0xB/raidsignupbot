package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/** Parsing and redirect helpers shared by the web forms. Times use the bot's time zone, like Discord. */
@Slf4j
final class WebForms {
  /** The raid durations Discord offers, as stored by {@code Event#getDuration()}. */
  static final List<String> DURATIONS = List.of("1h", "1,5h", "2h", "3h");

  private static final DateTimeFormatter DATE_TIME_INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

  private WebForms() {
  }

  static Duration duration(String value) {
    return switch (value == null ? "" : value) {
      case "1h" -> Duration.ofHours(1);
      case "1,5h" -> Duration.ofMinutes(90);
      case "2h" -> Duration.ofHours(2);
      case "3h" -> Duration.ofHours(3);
      default -> throw new IllegalArgumentException("Choose one of the durations " + String.join(", ", DURATIONS) + ".");
    };
  }

  static Instant dateTime(String value) {
    try {
      return LocalDateTime.parse(required(value, "start time")).atZone(ZoneId.systemDefault()).toInstant();
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException("Enter the start as date and time.");
    }
  }

  static String dateTimeInput(Instant instant) {
    return instant == null ? "" : DATE_TIME_INPUT.format(instant.atZone(ZoneId.systemDefault()));
  }

  static Instant startOfDay(String value) {
    try {
      return LocalDate.parse(required(value, "date")).atStartOfDay(ZoneId.systemDefault()).toInstant();
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException("Enter the date as YYYY-MM-DD.");
    }
  }

  static int nonNegative(String value, String field) {
    try {
      int number = Integer.parseInt(required(value, field).trim());
      if (number < 0) {
        throw new IllegalArgumentException(capitalize(field) + " must not be negative.");
      }
      return number;
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(capitalize(field) + " must be a whole number.");
    }
  }

  static String required(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Please fill in the " + field + ".");
    }
    return value.trim();
  }

  static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  static List<String> lines(String value) {
    return value == null ? List.of() : Arrays.stream(value.split("\\R")).map(String::trim).filter(line -> !line.isEmpty()).toList();
  }

  static Response redirect(String path, String parameter, String text) {
    return Response.seeOther(UriBuilder.fromPath(path).queryParam(parameter, text).build()).build();
  }

  /**
   * Runs a form action and redirects back with its success message, or with the error of a rejected
   * input. Unexpected failures are logged.
   */
  static Response run(String successPath, String errorPath, Action action) {
    try {
      return redirect(successPath, "message", action.run());
    } catch (IllegalArgumentException | IllegalStateException ex) {
      return redirect(errorPath, "error", ex.getMessage());
    } catch (RuntimeException ex) {
      log.error("Web action failed", ex);
      return redirect(errorPath, "error", "Something went wrong: " + ex.getMessage());
    }
  }

  static Response run(String path, Action action) {
    return run(path, path, action);
  }

  private static String capitalize(String value) {
    return Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }

  @FunctionalInterface
  interface Action {
    String run();
  }
}
