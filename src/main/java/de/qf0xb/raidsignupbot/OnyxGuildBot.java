package de.qf0xb.raidsignupbot;

import io.quarkus.runtime.QuarkusApplication;
import org.slf4j.Logger;

public class OnyxGuildBot implements QuarkusApplication {
  private final Logger logger = org.slf4j.LoggerFactory.getLogger(OnyxGuildBot.class);

  @Override
  public int run(String... args) {
    logger.info("Starting onyxguildbot");
    return 0;
  }
}
