package de.qf0xb.raidsignupbot.application.service.generator;

import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.event.EventSlot;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Improves the fallback's grouping without changing who is seated or their assignment counts. */
final class DualBoxTeamGrouper {
  private DualBoxTeamGrouper() {}

  static void group(List<EventTeam> teams, List<EventMemberSignup> signups) {
    Map<String, EventMemberSignup> byUser = signups.stream().collect(Collectors.toMap(
        signup -> signup.getGuildMember().getUserId(), Function.identity(), (first, ignored) -> first));
    while (improve(teams, byUser)) {
      // Each move strictly increases the number of players whose two boxes share a team.
    }
  }

  private static boolean improve(List<EventTeam> teams, Map<String, EventMemberSignup> signups) {
    long currentScore = groupedPlayers(teams, null, null);
    for (int leftIndex = 0; leftIndex < teams.size(); leftIndex++) {
      EventTeam left = teams.get(leftIndex);
      for (int rightIndex = leftIndex + 1; rightIndex < teams.size(); rightIndex++) {
        EventTeam right = teams.get(rightIndex);
        for (EventSlot leftSlot : left.getSlots()) {
          for (EventSlot rightSlot : right.getSlots()) {
            if (groupedPlayers(teams, leftSlot, rightSlot) <= currentScore
                || !canTake(signups.get(rightSlot.getMember().getUserId()), left, leftSlot)
                || !canTake(signups.get(leftSlot.getMember().getUserId()), right, rightSlot)) {
              continue;
            }
            try {
              left.swapSlots(leftSlot, right, rightSlot);
              return true;
            } catch (IllegalArgumentException ignored) {
              // A utility-role collision makes this particular exchange unsuitable.
            }
          }
        }
      }
    }
    return false;
  }

  private static boolean canTake(EventMemberSignup signup, EventTeam team, EventSlot slot) {
    return signup != null && signup.wantsRole(slot.getKey()) && team.utilitiesForSlot(slot).stream()
        .allMatch(utility -> signup.wantsRole(utility.getKey()));
  }

  private static long groupedPlayers(List<EventTeam> teams, EventSlot left, EventSlot right) {
    long count = 0;
    for (EventTeam team : teams) {
      Map<String, Integer> counts = new HashMap<>();
      for (EventSlot slot : team.getSlots()) {
        String userId = slot == left ? right.getMember().getUserId()
            : slot == right ? left.getMember().getUserId() : slot.getMember().getUserId();
        counts.merge(userId, 1, Integer::sum);
      }
      count += counts.values().stream().filter(value -> value == 2).count();
    }
    return count;
  }
}
