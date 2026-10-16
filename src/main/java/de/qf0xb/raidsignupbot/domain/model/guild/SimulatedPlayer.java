package de.qf0xb.raidsignupbot.domain.model.guild;

/**
 * Made-up players for team simulations. Their IDs look like Discord user IDs, so mentions and storage
 * work, but lie below 2^22: Discord IDs carry their creation time in the bits above that, so it never
 * hands out such small IDs. They can be recognized and skipped for Discord calls such as DMs and roles.
 */
public final class SimulatedPlayer {
  private static final long ID_BASE = 1_000_000L;
  private static final long ID_LIMIT = 1L << 22;

  private SimulatedPlayer() {
  }

  public static String userId(int number) {
    if (number < 1 || ID_BASE + number >= ID_LIMIT) {
      throw new IllegalArgumentException("Simulated player numbers must be between 1 and " + (ID_LIMIT - ID_BASE - 1));
    }
    return String.valueOf(ID_BASE + number);
  }

  public static boolean isSimulated(String userId) {
    try {
      long id = Long.parseLong(userId);
      return id > ID_BASE && id < ID_LIMIT;
    } catch (NumberFormatException ex) {
      return false;
    }
  }

  public static String displayName(String userId) {
    return String.format("Simulated %03d", Long.parseLong(userId) - ID_BASE);
  }
}
