package de.qf0xb.raidsignupbot;

import io.quarkus.runtime.QuarkusApplication;
import org.slf4j.Logger;

public class RaidSignupBot implements QuarkusApplication {
  Logger logger = org.slf4j.LoggerFactory.getLogger(RaidSignupBot.class);

  @Override
  public int run(String... args) throws Exception {
    logger.info("Starting Raid Signup Bot");
    return 0;
  }
}
