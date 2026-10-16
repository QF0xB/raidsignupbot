package de.qf0xb.raidsignupbot.infrastructure.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

// Named so web templates can read it as {inject:webConfig...}.
@Named("webConfig")
@ApplicationScoped
public class WebConfig {
  @ConfigProperty(name = "web.base-url")
  Optional<String> baseUrl;

  /** Test mode: lets event managers fill events with simulated players and send reminders early. */
  @ConfigProperty(name = "web.simulation.enabled", defaultValue = "false")
  boolean simulationEnabled;

  public boolean isSimulationEnabled() {
    return simulationEnabled;
  }

  /** Reminders test mode can send early, as offered in the web UI. */
  public java.util.List<String> getReminderStages() {
    return java.util.List.of("1h", "30m", "15m");
  }

  /** Public URL of the web UI without trailing slash, if it is reachable for Discord users. */
  public Optional<String> getBaseUrl() {
    return baseUrl == null ? Optional.empty()
        : baseUrl.map(String::trim).filter(value -> !value.isBlank()).map(value -> value.replaceAll("/+$", ""));
  }

  public Optional<String> eventUrl(Long eventId) {
    return getBaseUrl().map(base -> base + "/events/" + eventId);
  }
}
