package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class WebSecurityTest {
  @Test
  void pagesRequireALogin() {
    given().redirects().follow(false).when().get("/").then().statusCode(401);
    given().redirects().follow(false).when().get("/events/1").then().statusCode(401);
  }

  @Test
  void loggedOutPageAndStylesheetArePublic() {
    given().when().get("/logged-out").then().statusCode(200).body(containsString("Log in again"));
    given().when().get("/app.css").then().statusCode(200);
  }

  @Test
  @TestSecurity(user = "manager")
  void acceptsTemplateImportsLargerThanTheDefaultFormFieldLimit() {
    given().redirects().follow(false)
        .formParam("json", "x".repeat(100_000))
        .when().post("/servers/1/templates/import")
        .then().statusCode(not(413));
  }
}
