package io.mosip.idrepository.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import io.mosip.kernel.auth.defaultadapter.helper.TokenHelper;
import io.mosip.kernel.auth.defaultadapter.model.TokenHolder;

/**
 * Warms the kernel-auth service-account token cache at application startup.
 * <p>
 * Outbound REST ({@code selfTokenRestTemplate}, {@code selfTokenWebClient}) fails fast with
 * {@code Self cached auth token is null} when the first request races ahead of token fetch.
 * This listener pre-fetches a client-credentials token after the context is ready and logs
 * IAM configuration for local troubleshooting (without printing secrets).
 * </p>
 *
 * <h2>Property resolution order</h2>
 * <p>
 * For each of {@code clientid}, {@code clientsecret}, and {@code appid}:
 * </p>
 * <ol>
 *   <li>{@code mosip.iam.adapter.{key}.{spring.application.name}}</li>
 *   <li>{@code mosip.iam.adapter.{key}} (global)</li>
 * </ol>
 * <p>
 * Environment variables {@code MOSIP_IAM_ADAPTER_CLIENTID} / {@code MOSIP_IAM_ADAPTER_CLIENTSECRET}
 * override config-server values when bound by Spring Boot relaxed binding.
 * </p>
 *
 * <h2>Related configuration</h2>
 * <ul>
 *   <li>{@code auth.server.admin.issuer.uri} — external IAM token endpoint</li>
 *   <li>{@code auth.server.admin.issuer.internal.uri} — optional in-cluster issuer (logged when different)</li>
 *   <li>{@code mosip.kernel.auth.appids.realm.map} — realm lookup in {@link TokenHelper}</li>
 * </ul>
 *
 * @see IdRepoKernelAuthHelperConfig
 * @see io.mosip.kernel.auth.defaultadapter.config.SelfTokenRestInterceptor
 */
@Component
public class IdRepoSelfTokenStartupConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdRepoSelfTokenStartupConfig.class);

	private final Environment env;

	private final TokenHelper tokenHelper;

	private final TokenHolder<String> cachedToken;

	/**
	 * @param env         Spring environment (config server + local overrides)
	 * @param tokenHelper primary {@link TokenHelper} bean
	 * @param cachedToken shared token holder used by self-token REST/WebClient interceptors
	 */
	public IdRepoSelfTokenStartupConfig(Environment env, TokenHelper tokenHelper, TokenHolder<String> cachedToken) {
		this.env = env;
		this.tokenHelper = tokenHelper;
		this.cachedToken = cachedToken;
	}

	/**
	 * Runs once after the application context is ready; logs IAM settings and pre-fetches token if missing.
	 *
	 * @param event application ready event (unused; required for {@link EventListener} binding)
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void warmSelfTokenCache(ApplicationReadyEvent event) {
		String appName = env.getProperty("spring.application.name", "");
		ResolvedProperty clientIdProp = resolve(env, "mosip.iam.adapter.clientid", appName);
		ResolvedProperty clientSecretProp = resolve(env, "mosip.iam.adapter.clientsecret", appName);
		ResolvedProperty appIdProp = resolve(env, "mosip.iam.adapter.appid", appName);
		String clientId = clientIdProp.value();
		String clientSecret = clientSecretProp.value();
		String appId = StringUtils.hasText(appIdProp.value()) ? appIdProp.value() : appName;
		String issuer = env.getProperty("auth.server.admin.issuer.uri", "");
		String issuerInternal = env.getProperty("auth.server.admin.issuer.internal.uri", "");

		LOGGER.debug("Self-token IAM startup check for spring.application.name={}", appName);
		LOGGER.debug("  clientId: {} (from {})", maskEmpty(clientId), clientIdProp.source());
		LOGGER.debug("  clientSecret: {} (from {})", maskSecret(clientSecret), clientSecretProp.source());
		LOGGER.debug("  appId: {} (from {})", appId, appIdProp.source());
		LOGGER.debug("  auth.server.admin.issuer.uri: {}", StringUtils.hasText(issuer) ? issuer : "<missing>");
		LOGGER.debug("  auth.server.admin.issuer.internal.uri: {}",
				StringUtils.hasText(issuerInternal) ? issuerInternal : "<empty — uses issuer.uri>");
		if (StringUtils.hasText(issuerInternal) && StringUtils.hasText(issuer)
				&& !issuerInternal.trim().equals(issuer.trim())) {
			LOGGER.warn(
					"Internal issuer differs from external — laptop curl to iam.dev2 often works while internal URL returns 5xx");
		}

		if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
			LOGGER.error(
					"Self-token client credentials missing for app '{}'. Set MOSIP_IAM_ADAPTER_CLIENTID / "
							+ "MOSIP_IAM_ADAPTER_CLIENTSECRET or fix config server keys mosip.iam.adapter.clientid.{} "
							+ "and mosip.iam.adapter.clientsecret.{}",
					appName, appName, appName);
			return;
		}

		if (cachedToken.getToken() != null) {
			LOGGER.info("Self-token already present in cache — skipping startup pre-fetch");
			return;
		}

		LOGGER.debug("Self-token pre-fetch starting (clientId={}, appId={}) ...", clientId, appId);
		String token = tokenHelper.getClientToken(clientId, clientSecret, appId, (WebClient) null);
		if (token != null) {
			cachedToken.setToken(token);
			LOGGER.debug("Self-token pre-fetch succeeded for clientId={}", clientId);
		} else {
			LOGGER.error(
					"Self-token pre-fetch failed — see TokenHelper logs above (invalid_client = wrong id/secret). "
							+ "Local fix: set MOSIP_IAM_ADAPTER_CLIENTID=mosip-idrepo-client and "
							+ "MOSIP_IAM_ADAPTER_CLIENTSECRET from config server id-repository properties.");
		}
	}

	/**
	 * Resolves a config key with per-application suffix fallback.
	 *
	 * @param env     environment
	 * @param baseKey property prefix without trailing dot (for example {@code mosip.iam.adapter.clientid})
	 * @param appName {@code spring.application.name}
	 * @return resolved value and the property key it came from
	 */
	private static ResolvedProperty resolve(Environment env, String baseKey, String appName) {
		String perAppKey = baseKey + "." + appName;
		String perApp = env.getProperty(perAppKey);
		if (StringUtils.hasText(perApp)) {
			return new ResolvedProperty(perApp, perAppKey);
		}
		String global = env.getProperty(baseKey, "");
		return new ResolvedProperty(global, baseKey);
	}

	private static String maskEmpty(String value) {
		return StringUtils.hasText(value) ? value : "<missing>";
	}

	private static String maskSecret(String secret) {
		if (!StringUtils.hasText(secret)) {
			return "<missing>";
		}
		return "present (length=" + secret.length() + ")";
	}

	/**
	 * Config property value with the key it was read from (for startup diagnostics).
	 *
	 * @param value  property value (may be empty)
	 * @param source config key name
	 */
	private record ResolvedProperty(String value, String source) {
	}
}
