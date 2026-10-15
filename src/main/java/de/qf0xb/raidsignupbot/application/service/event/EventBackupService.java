package de.qf0xb.raidsignupbot.application.service.event;

import de.qf0xb.raidsignupbot.application.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventBackupOffer;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.scheduler.Scheduled;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class EventBackupService {
  private final Map<String, OfferState> offers = new ConcurrentHashMap<>();

  public synchronized void registerOffers(Event event, TeamGeneratorResult result) {
    if (event.getId() == null || event.getTemplate() == null || event.getEnd() == null) {
      return;
    }

    offers.entrySet().removeIf(entry -> entry.getValue().offer().eventId().equals(event.getId()));

    Map<String, String> primaryRoleNames = event.getTemplate().getStrategies().stream()
        .flatMap(strategy -> strategy.getPrimaryRoles().stream())
        .collect(Collectors.toMap(
            EventRole::getKey,
            EventRole::getName,
            (left, right) -> left,
            LinkedHashMap::new));

    for (EventMemberSignup signup : result.unmatchedSignups()) {
      signup.getRoleKeys().stream()
          .filter(primaryRoleNames::containsKey)
          .distinct()
          .sorted(Comparator.comparing(primaryRoleNames::get))
          .forEach(roleKey -> {
            EventBackupOffer offer = new EventBackupOffer(
                UUID.randomUUID().toString(),
                event.getId(),
                signup.getGuildMember().getUserId(),
                roleKey,
                primaryRoleNames.get(roleKey));
            offers.put(offer.offerId(), new OfferState(offer, OfferStatus.PENDING, event.getEnd()));
          });
    }
  }

  public List<EventBackupOffer> findOffers(Long eventId) {
    return offers.values().stream()
        .filter(state -> state.offer().eventId().equals(eventId))
        .map(OfferState::offer)
        .toList();
  }

  public synchronized BackupResponse respond(String offerId, String userId, boolean available) {
    OfferState state = offers.get(offerId);
    if (state == null || state.expiresAt().isBefore(Instant.now())) {
      throw new IllegalStateException("This backup request has expired");
    }
    if (!state.offer().userId().equals(userId)) {
      throw new IllegalArgumentException("This backup request belongs to another user");
    }
    List<OfferState> playerOffers = offers.values().stream()
        .filter(candidate -> candidate.offer().eventId().equals(state.offer().eventId()))
        .filter(candidate -> candidate.offer().userId().equals(userId))
        .sorted(Comparator.comparing(candidate -> candidate.offer().roleName()))
        .toList();
    if (playerOffers.stream().noneMatch(candidate -> candidate.status() == OfferStatus.PENDING)) {
      List<EventBackupOffer> accepted = playerOffers.stream()
          .filter(candidate -> candidate.status() == OfferStatus.ACCEPTED)
          .map(OfferState::offer)
          .toList();
      return BackupResponse.info(accepted.isEmpty()
          ? "This backup request has already been declined."
          : "You are already confirmed as the backup for " + formatRoles(accepted) + ".");
    }
    if (!available) {
      playerOffers.forEach(candidate -> candidate.setStatus(OfferStatus.DECLINED));
      return BackupResponse.info("You declined the backup request for this event.");
    }

    List<String> coveredRoles = findAcceptedOffers(state.offer().eventId()).stream()
        .map(EventBackupOffer::roleKey)
        .toList();
    List<EventBackupOffer> accepted = playerOffers.stream()
        .filter(candidate -> !coveredRoles.contains(candidate.offer().roleKey()))
        .map(OfferState::offer)
        .toList();
    playerOffers.forEach(candidate -> candidate.setStatus(
        coveredRoles.contains(candidate.offer().roleKey()) ? OfferStatus.DECLINED : OfferStatus.ACCEPTED));
    if (accepted.isEmpty()) {
      return BackupResponse.info("Someone else has already accepted all of these backup positions.");
    }

    return new BackupResponse("Thanks! You are confirmed as the backup for " + formatRoles(accepted) + ".", accepted);
  }

  private String formatRoles(List<EventBackupOffer> roleOffers) {
    return roleOffers.stream().map(EventBackupOffer::roleName).distinct()
        .map(name -> "**" + name + "**").collect(Collectors.joining(", "));
  }

  public List<EventBackupOffer> findAcceptedOffers(Long eventId) {
    return offers.values().stream()
        .filter(state -> state.offer().eventId().equals(eventId))
        .filter(state -> state.status() == OfferStatus.ACCEPTED)
        .map(OfferState::offer)
        .toList();
  }

  public synchronized void clearOffers(Long eventId) {
    offers.entrySet().removeIf(entry -> entry.getValue().offer().eventId().equals(eventId));
  }

  @Scheduled(every = "10m", identity = "event-backup-offer-cleanup")
  public void cleanupExpiredOffers() {
    Instant now = Instant.now();
    offers.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
  }

  /** Reply for the responding user and the positions newly filled by this response. */
  public record BackupResponse(String message, List<EventBackupOffer> acceptedOffers) {
    static BackupResponse info(String message) {
      return new BackupResponse(message, List.of());
    }
  }

  private enum OfferStatus {
    PENDING,
    ACCEPTED,
    DECLINED
  }

  private static final class OfferState {
    private final EventBackupOffer offer;
    private final Instant expiresAt;
    private OfferStatus status;

    private OfferState(EventBackupOffer offer, OfferStatus status, Instant expiresAt) {
      this.offer = offer;
      this.status = status;
      this.expiresAt = expiresAt;
    }

    private EventBackupOffer offer() {
      return offer;
    }

    private OfferStatus status() {
      return status;
    }

    private void setStatus(OfferStatus status) {
      this.status = status;
    }

    private Instant expiresAt() {
      return expiresAt;
    }
  }
}
