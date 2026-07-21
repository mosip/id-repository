package io.mosip.idrepository.core.httpfilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import jakarta.annotation.Resource;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import io.mosip.idrepository.core.logger.IdRepoLogger;
import io.mosip.idrepository.core.security.IdRepoSecurityManager;
import io.mosip.kernel.core.logger.spi.Logger;

/**
 * Abstract servlet {@link Filter} base for ID Repository HTTP request interception.
 *
 * <p>
 * Subclasses implement {@link #buildResponse(HttpServletRequest)} to validate or short-circuit
 * incoming requests before they reach controllers. Static assets, Swagger UI, and error
 * endpoints are excluded via {@link #allowedEndPoints()}. Request/response timing is logged
 * at DEBUG level using {@link IdRepoSecurityManager#getUser()} as the principal.
 * </p>
 *
 * <h2>Filter flow</h2>
 * <ol>
 *   <li>Record request start time and URL</li>
 *   <li>If {@link #shouldNotFilter(HttpServletRequest)} — pass through {@link FilterChain}</li>
 *   <li>Else call {@link #buildResponse(HttpServletRequest)}</li>
 *   <li>If response string is {@code null} — continue the chain; otherwise write it and
 *       short-circuit</li>
 *   <li>Log elapsed duration at DEBUG</li>
 * </ol>
 *
 * <h2>Excluded paths</h2>
 * <p>
 * Ant patterns for assets, favicon, CSS/JS, error pages, webjars, and Swagger
 * ({@code /v2/api-docs}, {@code /swagger-ui.html}, etc.). Matching uses
 * {@link HttpServletRequest#getPathInfo()} via {@link AntPathMatcher}. When
 * {@code pathInfo} is {@code null}, the request is not filtered (pass-through).
 * </p>
 *
 * <h2>Subclassing</h2>
 * <pre>
 * public final class IdRepoFilter extends BaseIdRepoFilter {
 *     {@literal @}Override
 *     protected String buildResponse(HttpServletRequest request) {
 *         // return JSON error body to short-circuit, or null to continue
 *         return null;
 *     }
 * }
 * </pre>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code io.mosip.idrepository.identity.httpfilter.IdRepoFilter} — identity API
 *       filter in the service module</li>
 * </ul>
 *
 * <h2>Related filters</h2>
 * <p>
 * {@link JsonRequestSanitizingFilter} runs separately (high precedence) to clean JSON
 * bodies before Jackson parsing; it does not extend this class.
 * </p>
 *
 * @author Manoj SP
 * @author Prem Kumar
 * @see Filter
 * @see AntPathMatcher
 * @see JsonRequestSanitizingFilter
 * @see IdRepoSecurityManager#getUser()
 */
@Component
public abstract class BaseIdRepoFilter implements Filter  {
	/** Logger category identifier for filter operations. */
	private static final String ID_REPO_FILTER = "IdRepoFilter";

	/** Application module name for structured logging. */
	private static final String ID_REPO = "IdRepo";

	/** Structured logger for filter request/response timing. */
	private static Logger mosipLogger = IdRepoLogger.getLogger(BaseIdRepoFilter.class);

	/** Ant-style path matcher for allowed-endpoint exclusion checks. */
	AntPathMatcher pathMatcher = new AntPathMatcher();

	/**
	 * Map of operation names to MOSIP response {@code id} values.
	 * <p>
	 * Injected for subclasses that build MOSIP error envelopes needing a configured
	 * response {@code id}.
	 * </p>
	 */
	@Resource
	private Map<String, String> id;

	/**
	 * UIN value extracted by subclasses during request validation.
	 * <p>
	 * May remain unused in the base class; retained for subclass state during filter
	 * processing.
	 * </p>
	 */
	String uin;

	/**
	 * Filter initialization hook — no-op in the base implementation.
	 *
	 * @param filterConfig servlet filter configuration from the container
	 * @throws ServletException if a subclass override fails initialization (base never
	 *                          throws)
	 */
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}
	
	/**
	 * Returns {@code true} when this filter should skip validation for the request.
	 * <p>
	 * Matches {@link HttpServletRequest#getPathInfo()} against
	 * {@link #allowedEndPoints()}. If {@code pathInfo} is {@code null}, returns
	 * {@code true} (do not filter).
	 * </p>
	 *
	 * @param request incoming HTTP request
	 * @return {@code true} to pass through without calling {@link #buildResponse};
	 *         {@code false} to validate
	 */
	private boolean shouldNotFilter(HttpServletRequest request) {
		if (Objects.nonNull(request.getPathInfo())) {
			return Arrays.stream(allowedEndPoints()).anyMatch(p -> pathMatcher.match(p, request.getPathInfo()));
		}
		return true;
	}

	/**
	 * Ant-style path patterns that bypass filter validation.
	 * <p>
	 * Covers static assets, favicon, CSS/JS, error pages, webjars, and Swagger UI /
	 * OpenAPI documentation endpoints.
	 * </p>
	 *
	 * @return array of Ant path patterns; never {@code null}
	 */
	private String[] allowedEndPoints() {
		return new String[] { "/assets/**", "/icons/**", "/screenshots/**", "/favicon**", "/**/favicon**", "/css/**",
				"/js/**", "/**/error**", "/**/webjars/**", "/**/v2/api-docs", "/**/configuration/ui",
				"/**/configuration/security", "/**/swagger-resources/**", "/**/swagger-ui.html" };
	}
	
	/**
	 * Servlet filter entry point — logs timing and delegates to subclass validation.
	 * <p>
	 * If {@link #shouldNotFilter(HttpServletRequest)} returns {@code true}, passes through
	 * unchanged. Otherwise calls {@link #buildResponse(HttpServletRequest)}; a non-null
	 * response is written directly to {@link ServletResponse#getWriter()} and the filter
	 * chain is short-circuited. Timing (ms and seconds) is always logged at DEBUG after
	 * processing.
	 * </p>
	 *
	 * @param request  incoming servlet request (cast to {@link HttpServletRequest})
	 * @param response servlet response
	 * @param chain    remaining filter chain
	 * @throws IOException      if response writing or chain processing fails
	 * @throws ServletException if filter processing fails
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		Instant requestTime = Instant.now();
		mosipLogger.debug(IdRepoSecurityManager.getUser(), ID_REPO, ID_REPO_FILTER, "Request Received at: " + requestTime);
		mosipLogger.debug(IdRepoSecurityManager.getUser(), ID_REPO, ID_REPO_FILTER, "Request URL: " + ((HttpServletRequest) request).getRequestURL());

		mosipLogger.debug(IdRepoSecurityManager.getUser(), ID_REPO, ID_REPO_FILTER, "Request received");
		if (shouldNotFilter((HttpServletRequest) request)) {
			chain.doFilter(request, response);
		} else {
			String responseString = buildResponse((HttpServletRequest) request);
			if (Objects.isNull(responseString)) {
				chain.doFilter(request, response);
			} else {
				response.getWriter().write(responseString);
			}
		}
		Instant responseTime = Instant.now();
		mosipLogger.debug(IdRepoSecurityManager.getUser(), ID_REPO, ID_REPO_FILTER, "Response sent at: " + responseTime);
		long duration = Duration.between(requestTime, responseTime).toMillis();
		mosipLogger.debug(IdRepoSecurityManager.getUser(), ID_REPO, ID_REPO_FILTER,
				"Time taken to respond in ms: " + duration
						+ ". Time difference between request and response in Seconds: " + ((double) duration / 1000)
						+ " for url : " + ((HttpServletRequest) request).getRequestURL() + " method: "
						+ ((HttpServletRequest) request).getMethod());
	}

	/**
	 * Builds an early HTTP response when request validation fails.
	 * <p>
	 * Return {@code null} to allow the request to proceed through the filter chain.
	 * Return a JSON (or other) body string to write it to the response and skip
	 * downstream filters/controllers.
	 * </p>
	 *
	 * @param request the incoming HTTP servlet request
	 * @return error response body to write, or {@code null} to pass through
	 */
	protected abstract String buildResponse(HttpServletRequest request);

	/**
	 * Filter destruction hook — no-op in the base implementation.
	 * <p>
	 * Subclasses may override to release resources held for the filter lifetime.
	 * </p>
	 */
	@Override
	public void destroy() {
	}
}
