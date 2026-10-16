package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import de.qf0xb.raidsignupbot.application.service.template.EventStrategyService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateImportExportService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateService;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Web counterpart of /edittemplate, /exporttemplate(s), and /importtemplates. */
@Path("/servers/{serverId}/templates")
@Authenticated
@Blocking
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class TemplateResource {
  @CheckedTemplate
  static class Templates {
    static native TemplateInstance templates(
        String userName, WebAccessService.ServerAccess server, List<EventTemplate> templates, String message, String error);

    static native TemplateInstance template(
        String userName, WebAccessService.ServerAccess server, EventTemplate template, String message, String error);
  }

  private final CurrentUser currentUser;
  private final WebAccessService accessService;
  private final TemplateService templateService;
  private final EventStrategyService strategyService;
  private final TemplateImportExportService importExportService;

  public TemplateResource(
      CurrentUser currentUser,
      WebAccessService accessService,
      TemplateService templateService,
      EventStrategyService strategyService,
      TemplateImportExportService importExportService) {
    this.currentUser = currentUser;
    this.accessService = accessService;
    this.templateService = templateService;
    this.strategyService = strategyService;
    this.importExportService = importExportService;
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance list(
      @PathParam("serverId") String serverId,
      @QueryParam("message") String message,
      @QueryParam("error") String error) {
    WebAccessService.ServerAccess access = requireTemplateManager(serverId);
    List<EventTemplate> templates = templateService.findAllTemplates(serverId).stream()
        .sorted(Comparator.comparing(EventTemplate::getName, String.CASE_INSENSITIVE_ORDER))
        .toList();
    return Templates.templates(currentUser.displayName(), access, templates, message, error);
  }

  @POST
  public Response create(
      @PathParam("serverId") String serverId,
      @FormParam("key") String key,
      @FormParam("name") String name,
      @FormParam("emojiSlug") String emojiSlug,
      @FormParam("details") String details) {
    requireTemplateManager(serverId);
    return WebForms.run(listPath(serverId), () -> {
      String templateKey = WebForms.required(key, "key");
      if (templateService.findAllTemplates(serverId).stream().anyMatch(template -> template.getKey().equals(templateKey))) {
        throw new IllegalArgumentException("A template with key " + templateKey + " already exists.");
      }
      templateService.createTemplate(serverId, templateKey, WebForms.required(name, "name"),
          WebForms.required(details, "details"), WebForms.required(emojiSlug, "emoji"));
      return "Template " + templateKey + " created.";
    });
  }

  @POST
  @Path("/import")
  public Response importTemplates(@PathParam("serverId") String serverId, @FormParam("json") String json) {
    requireTemplateManager(serverId);
    return WebForms.run(listPath(serverId), () -> {
      List<EventTemplate> imported = importExportService.importTemplates(serverId,
          new ByteArrayInputStream(WebForms.required(json, "template JSON").getBytes(StandardCharsets.UTF_8)));
      return "Imported " + imported.size() + " template(s).";
    });
  }

  @GET
  @Path("/export")
  @Produces(MediaType.APPLICATION_JSON)
  public Response exportAll(@PathParam("serverId") String serverId) {
    requireTemplateManager(serverId);
    return download(importExportService.exportTemplates(templateService.findAllTemplates(serverId)),
        "templates-export-" + LocalDate.now() + ".json");
  }

  @GET
  @Path("/{key}")
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance edit(
      @PathParam("serverId") String serverId,
      @PathParam("key") String key,
      @QueryParam("message") String message,
      @QueryParam("error") String error) {
    WebAccessService.ServerAccess access = requireTemplateManager(serverId);
    return Templates.template(currentUser.displayName(), access, templateService.findTemplateByKey(serverId, key),
        message, error);
  }

  @GET
  @Path("/{key}/export")
  @Produces(MediaType.APPLICATION_JSON)
  public Response export(@PathParam("serverId") String serverId, @PathParam("key") String key) {
    requireTemplateManager(serverId);
    return download(importExportService.exportTemplate(templateService.findTemplateByKey(serverId, key)),
        "template-" + key + ".json");
  }

  @POST
  @Path("/{key}")
  public Response update(
      @PathParam("serverId") String serverId,
      @PathParam("key") String key,
      @FormParam("name") String name,
      @FormParam("emojiSlug") String emojiSlug,
      @FormParam("details") String details) {
    requireTemplateManager(serverId);
    return WebForms.run(path(serverId, key), () -> {
      templateService.updateTemplate(serverId, key, WebForms.required(name, "name"),
          WebForms.required(emojiSlug, "emoji"), WebForms.required(details, "details"));
      return "Template saved.";
    });
  }

  @POST
  @Path("/{key}/strategies")
  public Response addStrategy(
      @PathParam("serverId") String serverId,
      @PathParam("key") String key,
      @FormParam("strategyKey") String strategyKey,
      @FormParam("name") String name,
      @FormParam("guides") String guides) {
    requireTemplateManager(serverId);
    return WebForms.run(path(serverId, key), () -> {
      String newKey = WebForms.required(strategyKey, "strategy key");
      if (templateService.findTemplateByKey(serverId, key).findStrategy(newKey).isPresent()) {
        throw new IllegalArgumentException("Strategy " + newKey + " already exists.");
      }
      strategyService.addStrategy(serverId, key, newKey, WebForms.required(name, "strategy name"));
      strategyService.updateStrategy(serverId, key, newKey, name.trim(), WebForms.lines(guides));
      return "Strategy " + newKey + " added.";
    });
  }

  @POST
  @Path("/{key}/strategies/{strategyKey}")
  public Response updateStrategy(
      @PathParam("serverId") String serverId,
      @PathParam("key") String key,
      @PathParam("strategyKey") String strategyKey,
      @FormParam("name") String name,
      @FormParam("guides") String guides) {
    requireTemplateManager(serverId);
    return WebForms.run(path(serverId, key), () -> {
      strategyService.updateStrategy(serverId, key, strategyKey, WebForms.required(name, "strategy name"),
          WebForms.lines(guides));
      return "Strategy " + strategyKey + " saved.";
    });
  }

  @POST
  @Path("/{key}/strategies/{strategyKey}/delete")
  public Response removeStrategy(
      @PathParam("serverId") String serverId,
      @PathParam("key") String key,
      @PathParam("strategyKey") String strategyKey) {
    requireTemplateManager(serverId);
    return WebForms.run(path(serverId, key), () -> {
      strategyService.removeStrategy(serverId, key, strategyKey);
      return "Strategy " + strategyKey + " removed.";
    });
  }

  @POST
  @Path("/{key}/strategies/{strategyKey}/roles")
  public Response addRole(
      @PathParam("serverId") String serverId,
      @PathParam("key") String key,
      @PathParam("strategyKey") String strategyKey,
      @FormParam("roleKey") String roleKey,
      @FormParam("utility") String utility,
      @FormParam("name") String name,
      @FormParam("emojiSlug") String emojiSlug,
      @FormParam("requiredCount") String requiredCount,
      @FormParam("eligible") List<String> eligible) {
    requireTemplateManager(serverId);
    return WebForms.run(path(serverId, key), () -> {
      String newKey = WebForms.required(roleKey, "role key");
      if (templateService.findTemplateByKey(serverId, key).requireStrategy(strategyKey).findRole(newKey).isPresent()) {
        throw new IllegalArgumentException("Role " + newKey + " already exists in this strategy.");
      }
      int count = WebForms.nonNegative(requiredCount, "required count");
      if ("true".equals(utility)) {
        strategyService.addUtilityRole(serverId, key, strategyKey, newKey, WebForms.required(name, "role name"),
            WebForms.required(emojiSlug, "emoji"), count, eligibleKeys(eligible));
      } else {
        strategyService.addPrimaryRole(serverId, key, strategyKey, newKey, WebForms.required(name, "role name"),
            WebForms.required(emojiSlug, "emoji"), count);
      }
      return "Role " + newKey + " added.";
    });
  }

  @POST
  @Path("/{key}/strategies/{strategyKey}/roles/{roleKey}")
  public Response updateRole(
      @PathParam("serverId") String serverId,
      @PathParam("key") String key,
      @PathParam("strategyKey") String strategyKey,
      @PathParam("roleKey") String roleKey,
      @FormParam("name") String name,
      @FormParam("emojiSlug") String emojiSlug,
      @FormParam("requiredCount") String requiredCount,
      @FormParam("eligible") List<String> eligible) {
    requireTemplateManager(serverId);
    return WebForms.run(path(serverId, key), () -> {
      strategyService.updateRole(serverId, key, strategyKey, roleKey, WebForms.required(name, "role name"),
          WebForms.required(emojiSlug, "emoji"), WebForms.nonNegative(requiredCount, "required count"),
          eligibleKeys(eligible));
      return "Role " + roleKey + " saved.";
    });
  }

  @POST
  @Path("/{key}/strategies/{strategyKey}/roles/{roleKey}/delete")
  public Response removeRole(
      @PathParam("serverId") String serverId,
      @PathParam("key") String key,
      @PathParam("strategyKey") String strategyKey,
      @PathParam("roleKey") String roleKey) {
    requireTemplateManager(serverId);
    return WebForms.run(path(serverId, key), () -> {
      strategyService.removeRole(serverId, key, strategyKey, roleKey);
      return "Role " + roleKey + " removed.";
    });
  }

  private WebAccessService.ServerAccess requireTemplateManager(String serverId) {
    WebAccessService.ServerAccess access = accessService.serverAccess(serverId, currentUser.id());
    access.require(access.manageTemplates());
    return access;
  }

  private static Set<String> eligibleKeys(List<String> eligible) {
    return eligible == null ? Set.of() : new HashSet<>(eligible);
  }

  private static Response download(byte[] json, String fileName) {
    return Response.ok(json, MediaType.APPLICATION_JSON_TYPE)
        .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
        .build();
  }

  private static String listPath(String serverId) {
    return "/servers/" + serverId + "/templates";
  }

  private static String path(String serverId, String key) {
    return listPath(serverId) + "/" + key;
  }
}
