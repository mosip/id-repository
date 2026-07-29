package io.mosip.idrepository.config;

import static io.mosip.idrepository.common.constant.IdRepoApiPathConstants.CREDENTIAL_REQUEST_PATH_PREFIX;
import static io.mosip.idrepository.common.constant.IdRepoApiPathConstants.CREDENTIAL_SERVICE_PATH_PREFIX;
import static io.mosip.idrepository.common.constant.IdRepoApiPathConstants.IDENTITY_PATH_PREFIX;
import static io.mosip.idrepository.common.constant.IdRepoApiPathConstants.VID_PATH_PREFIX;

import java.util.List;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.mosip.idrepository.common.constant.IdRepoApiPathConstants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import jakarta.servlet.http.HttpServletRequest;

/**
 * OpenAPI 3 / SpringDoc for the consolidated ID-Repository deployable.
 * <p>
 * Each former microservice keeps its own Swagger UI under the legacy public path prefix
 * (same shape as pre-consolidation), instead of one grouped UI at {@code /swagger-ui/index.html}.
 * </p>
 *
 * <h2>Per-service Swagger UI</h2>
 * <table>
 *   <caption>Swagger UI and OpenAPI document URLs (legacy paths)</caption>
 *   <tr><th>Service</th><th>Swagger UI</th><th>OpenAPI JSON</th></tr>
 *   <tr><td>Identity</td><td>{@code /idrepository/v1/identity/swagger-ui/index.html}</td>
 *       <td>{@code /idrepository/v1/identity/v3/api-docs}</td></tr>
 *   <tr><td>VID</td><td>{@code /idrepository/v1/swagger-ui/index.html}</td>
 *       <td>{@code /idrepository/v1/v3/api-docs}</td></tr>
 *   <tr><td>Credential store</td><td>{@code /v1/credentialservice/swagger-ui/index.html}</td>
 *       <td>{@code /v1/credentialservice/v3/api-docs}</td></tr>
 *   <tr><td>Credential request</td><td>{@code /v1/credentialrequest/swagger-ui/index.html}</td>
 *       <td>{@code /v1/credentialrequest/v3/api-docs}</td></tr>
 * </table>
 *
 * @see IdRepoApiPathConfig
 * @see IdRepoApiPathConstants
 */
@Configuration
public class IdRepoOpenApiConfig implements WebMvcConfigurer {

	/** Mount points: UI base path (no trailing slash), SpringDoc group id, page title. */
	public static final List<SwaggerMount> SWAGGER_MOUNTS = List.of(
			new SwaggerMount(IDENTITY_PATH_PREFIX, "identity", "MOSIP ID-Repository Identity"),
			new SwaggerMount(VID_PATH_PREFIX, "vid", "MOSIP ID-Repository VID"),
			new SwaggerMount(CREDENTIAL_SERVICE_PATH_PREFIX, "credential-service", "MOSIP Credential Store"),
			new SwaggerMount(CREDENTIAL_REQUEST_PATH_PREFIX, "credential-request",
					"MOSIP Credential Request Generator"));

	/**
	 * @param uiBasePath service public prefix (e.g. {@code /idrepository/v1/identity})
	 * @param groupId    SpringDoc {@link GroupedOpenApi} group name
	 * @param title      HTML / OpenAPI title
	 */
	public record SwaggerMount(String uiBasePath, String groupId, String title) {

		/** @return {@code {uiBasePath}/swagger-ui/index.html} */
		public String swaggerUiIndexPath() {
			return uiBasePath + "/swagger-ui/index.html";
		}

		/**
		 * Public OpenAPI JSON path (legacy per-service URL).
		 *
		 * @return {@code {uiBasePath}/v3/api-docs}
		 */
		public String apiDocsPath() {
			return uiBasePath + "/v3/api-docs";
		}

		/** Internal SpringDoc group document forwarded from {@link #apiDocsPath()}. */
		public String springDocGroupPath() {
			return "/v3/api-docs/" + groupId;
		}
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		for (SwaggerMount mount : SWAGGER_MOUNTS) {
			String index = mount.swaggerUiIndexPath();
			registry.addRedirectViewController(mount.uiBasePath() + "/swagger-ui.html", index);
			registry.addRedirectViewController(mount.uiBasePath() + "/swagger-ui", index);
			registry.addRedirectViewController(mount.uiBasePath() + "/swagger-ui/", index);
			// Keep browser URL as legacy .../v3/api-docs; forward to SpringDoc group document
			registry.addViewController(mount.uiBasePath() + "/v3/api-docs")
					.setViewName("forward:" + mount.springDocGroupPath());
			registry.addViewController(mount.uiBasePath() + "/v3/api-docs/")
					.setViewName("forward:" + mount.springDocGroupPath());
		}
		// Root UI → identity (per-service UIs are canonical)
		registry.addRedirectViewController("/swagger-ui.html", IDENTITY_PATH_PREFIX + "/swagger-ui/index.html");
		registry.addRedirectViewController("/swagger-ui", IDENTITY_PATH_PREFIX + "/swagger-ui/index.html");
		registry.addRedirectViewController("/swagger-ui/", IDENTITY_PATH_PREFIX + "/swagger-ui/index.html");
		registry.addRedirectViewController("/swagger-ui/index.html", IDENTITY_PATH_PREFIX + "/swagger-ui/index.html");
	}

	/**
	 * Shared OpenAPI shell; each {@link GroupedOpenApi} overrides {@link Info} via customizer.
	 *
	 * @return base OpenAPI bean required by SpringDoc
	 */
	@Bean
	public OpenAPI idRepositoryOpenApi() {
		return new OpenAPI()
				.components(new Components())
				.info(new Info()
						.title("MOSIP ID-Repository")
						.description("Use per-service Swagger UI under each API path prefix")
						.version("1.2.1")
						.license(new License().name("MPL 2.0").url("https://www.mozilla.org/en-US/MPL/2.0/")));
	}

	@Bean
	public GroupedOpenApi identityApi() {
		return GroupedOpenApi.builder()
				.group("identity")
				.displayName("Id Repository Identity Service")
				.packagesToScan("io.mosip.idrepository.identity.controller")
				.pathsToMatch(IDENTITY_PATH_PREFIX + "/**")
				.addOpenApiCustomizer(openApi -> openApi.info(serviceInfo("MOSIP ID-Repository Identity Service",
						"Identity and draft identity APIs")))
				.build();
	}

	@Bean
	public GroupedOpenApi credentialServiceApi() {
		return GroupedOpenApi.builder()
				.group("credential-service")
				.displayName("Credential Store")
				.packagesToScan("io.mosip.idrepository.credential.store.controller")
				.pathsToMatch(CREDENTIAL_SERVICE_PATH_PREFIX + "/**")
				.addOpenApiCustomizer(openApi -> openApi.info(serviceInfo("MOSIP Credential Store",
						"Credential issuance APIs")))
				.build();
	}

	@Bean
	public GroupedOpenApi credentialRequestApi() {
		return GroupedOpenApi.builder()
				.group("credential-request")
				.displayName("Credential Request Generator")
				.packagesToScan("io.mosip.idrepository.credential.request.controller")
				.pathsToMatch(CREDENTIAL_REQUEST_PATH_PREFIX + "/**")
				.addOpenApiCustomizer(openApi -> openApi.info(serviceInfo("MOSIP Credential Request Generator",
						"Credential request queue APIs")))
				.build();
	}

	@Bean
	public GroupedOpenApi vidApi() {
		return GroupedOpenApi.builder()
				.group("vid")
				.displayName("Id Repo VID Service")
				.packagesToScan("io.mosip.idrepository.vid.controller")
				.pathsToMatch(VID_PATH_PREFIX + "/vid", VID_PATH_PREFIX + "/vid/**")
				.addOpenApiCustomizer(openApi -> openApi.info(serviceInfo("MOSIP ID-Repository VID Service",
						"VID lifecycle APIs")))
				.build();
	}

	private static Info serviceInfo(String title, String description) {
		return new Info()
				.title(title)
				.description(description)
				.version("1.2.1")
				.license(new License().name("MPL 2.0").url("https://www.mozilla.org/en-US/MPL/2.0/"));
	}

	/**
	 * Serves a dedicated Swagger UI HTML page under each service prefix.
	 * <p>
	 * Static assets load from SpringDoc’s root {@code /swagger-ui/*} webjars; each page pins
	 * {@code url} to that service’s legacy {@code {prefix}/v3/api-docs} document (no group dropdown).
	 * </p>
	 */
	@Controller
	static class PerServiceSwaggerUiController {

		@GetMapping(path = {
				IDENTITY_PATH_PREFIX + "/swagger-ui/index.html",
				VID_PATH_PREFIX + "/swagger-ui/index.html",
				CREDENTIAL_SERVICE_PATH_PREFIX + "/swagger-ui/index.html",
				CREDENTIAL_REQUEST_PATH_PREFIX + "/swagger-ui/index.html"
		}, produces = MediaType.TEXT_HTML_VALUE)
		@ResponseBody
		public String swaggerUiIndex(HttpServletRequest request) {
			SwaggerMount mount = resolveMount(request.getRequestURI())
					.orElse(SWAGGER_MOUNTS.get(0));
			return renderSwaggerHtml(mount);
		}

		private static java.util.Optional<SwaggerMount> resolveMount(String requestUri) {
			String path = requestUri == null ? "" : requestUri;
			// Longest prefix first so /idrepository/v1/identity wins over /idrepository/v1
			return SWAGGER_MOUNTS.stream()
					.sorted((a, b) -> Integer.compare(b.uiBasePath().length(), a.uiBasePath().length()))
					.filter(m -> path.startsWith(m.uiBasePath() + "/swagger-ui"))
					.findFirst();
		}

		private static String renderSwaggerHtml(SwaggerMount mount) {
			String title = escape(mount.title());
			String apiDocs = escape(mount.apiDocsPath());
			// Absolute asset URLs from SpringDoc’s default swagger-ui resource mapping
			return """
					<!DOCTYPE html>
					<html lang="en">
					<head>
					  <meta charset="UTF-8">
					  <title>%s</title>
					  <link rel="stylesheet" type="text/css" href="/swagger-ui/swagger-ui.css">
					  <link rel="icon" type="image/png" href="/swagger-ui/favicon-32x32.png" sizes="32x32">
					</head>
					<body>
					  <div id="swagger-ui"></div>
					  <script src="/swagger-ui/swagger-ui-bundle.js"></script>
					  <script src="/swagger-ui/swagger-ui-standalone-preset.js"></script>
					  <script>
					    window.onload = function() {
					      window.ui = SwaggerUIBundle({
					        url: '%s',
					        dom_id: '#swagger-ui',
					        deepLinking: true,
					        presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
					        plugins: [SwaggerUIBundle.plugins.DownloadUrl],
					        layout: 'StandaloneLayout',
					        validatorUrl: ''
					      });
					    };
					  </script>
					</body>
					</html>
					""".formatted(title, apiDocs);
		}

		private static String escape(String value) {
			return value.replace("'", "\\'").replace("<", "&lt;").replace(">", "&gt;");
		}
	}
}
