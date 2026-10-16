package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import io.quarkus.oidc.UserInfo;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.NotAuthorizedException;

/** The Discord user behind the current web session, taken from Discord's /users/@me response. */
@RequestScoped
public class CurrentUser {
  private final UserInfo userInfo;

  public CurrentUser(UserInfo userInfo) {
    this.userInfo = userInfo;
  }

  public String id() {
    String id = userInfo.getString("id");
    if (id == null || id.isBlank()) {
      throw new NotAuthorizedException("Discord did not return a user id.");
    }
    return id;
  }

  public String displayName() {
    String globalName = userInfo.getString("global_name");
    return globalName != null && !globalName.isBlank() ? globalName : userInfo.getString("username");
  }
}
