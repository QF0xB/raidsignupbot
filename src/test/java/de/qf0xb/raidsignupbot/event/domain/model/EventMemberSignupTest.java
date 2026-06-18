package de.qf0xb.raidsignupbot.event.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventMemberSignupTest {
  private EventMemberSignup eventMemberSignup;

  @BeforeEach
  void setUp() {
    eventMemberSignup = new EventMemberSignup(null);

    eventMemberSignup.addRole(new EventRole("primary", 1));
    eventMemberSignup.addRole(new EventRole("utility", 1, true));
  }

  @Test
  void getPrimaryRoles() {
    assertEquals(1, eventMemberSignup.getPrimaryRoles().size());
    assertEquals("primary", eventMemberSignup.getPrimaryRoles().getFirst().getName());
  }

  @Test
  void getUtilities() {
    assertEquals(1, eventMemberSignup.getUtilities().size());
    assertEquals("utility", eventMemberSignup.getUtilities().getFirst().getName());
  }
}
