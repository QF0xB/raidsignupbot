package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import io.quarkus.oidc.OidcSession;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;

/** Discord has no logout endpoint, so logging out only drops the local session cookie. */
@Path("/")
public class SessionResource {
  @CheckedTemplate
  static class Templates {
    static native TemplateInstance loggedOut();
  }

  private final OidcSession oidcSession;

  public SessionResource(OidcSession oidcSession) {
    this.oidcSession = oidcSession;
  }

  /** Discord redirects here after login; Quarkus then restores the page the user originally requested. */
  @GET
  @Path("auth/callback")
  @Authenticated
  public Response callback() {
    return Response.seeOther(URI.create("/")).build();
  }

  @POST
  @Path("logout")
  @Authenticated
  public Uni<Response> logout() {
    return oidcSession.logout().replaceWith(Response.seeOther(URI.create("/logged-out")).build());
  }

  @GET
  @Path("logged-out")
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance loggedOut() {
    return Templates.loggedOut();
  }
}
