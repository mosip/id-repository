package io.mosip.idrepository.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import io.mosip.kernel.auth.defaultadapter.config.NoAuthenticationEndPoint;
import io.mosip.kernel.auth.defaultadapter.model.GlobalEndPoint;
import io.mosip.kernel.auth.defaultadapter.model.ServiceEndPoint;
import io.mosip.kernel.auth.defaultadapter.filter.AuthFilter;
import io.mosip.kernel.auth.defaultadapter.filter.CorsFilter;
import io.mosip.kernel.auth.defaultadapter.handler.AuthHandler;
import io.mosip.kernel.auth.defaultadapter.handler.AuthSuccessHandler;
import io.mosip.kernel.core.util.EmptyCheckUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Security 7 filter chain for the consolidated ID-Repository service.
 * <p>
 * Replaces {@code io.mosip.kernel.auth.defaultadapter.config.SecurityConfig} from
 * {@code kernel-auth-adapter}, which relies on removed {@code AntPathRequestMatcher}.
 * The kernel {@code SecurityConfig} is excluded from component scan in
 * {@link HttpModeScanConfiguration}.
 * </p>
 * <p>
 * Auth exclusions from config server ({@code mosip.global.end-points},
 * {@code mosip.service.end-points}) are matched with {@link AntPathMatcher} because Boot 4
 * {@code PathPattern} parsing rejects legacy multi-{@code **} Ant patterns used in MOSIP sandboxes.
 * </p>
 *
 * <h2>Configuration properties</h2>
 * <ul>
 *   <li>{@code mosip.security.csrf-enable} — enable CSRF (default {@code false})</li>
 *   <li>{@code mosip.kernel.csrf_ignore.url} — Ant patterns exempt from CSRF when enabled</li>
 *   <li>{@code mosip.security.cors-enable} — enable kernel {@code CorsFilter}</li>
 *   <li>{@code mosip.security.origins} — allowed CORS origins</li>
 *   <li>{@code mosip.security.authentication.provider.beans.list.{appName}} — extra auth providers</li>
 * </ul>
 *
 * @see io.mosip.kernel.auth.defaultadapter.filter.AuthFilter
 * @see io.mosip.idrepository.config.IdRepoKernelAuthHelperConfig
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Order(2)
public class KernelAuthSecurityConfig {

	/** Ant patterns exempt from CSRF when {@code mosip.security.csrf-enable=true}. */
	@Value("${mosip.kernel.csrf_ignore.url:}")
	private String[] csrfIgnoreUrls;

	/** When {@code true}, CSRF protection is enabled with cookie token repository. */
	@Value("${mosip.security.csrf-enable:false}")
	private boolean isCSRFEnable;

	/** When {@code true}, kernel {@link CorsFilter} is inserted before {@link AuthFilter}. */
	@Value("${mosip.security.cors-enable:false}")
	private boolean isCORSEnable;

	/** Comma-separated allowed origins for CORS (used when CORS is enabled). */
	@Value("${mosip.security.origins:localhost:8080}")
	private String origins;

	/** Resolves optional per-app authentication provider beans by name. */
	@Autowired
	private ApplicationContext applicationContext;

	/** Kernel MOSIP JWT/cookie authentication provider. */
	@Autowired
	private AuthHandler authProvider;

	/** Reads {@code spring.application.name} and security property keys. */
	@Autowired
	private Environment environment;

	/** Config-server bean listing global and service-specific no-auth URL patterns. */
	@Autowired
	private NoAuthenticationEndPoint noAuthenticationEndPoint;

	/**
	 * Builds the authentication manager from optional custom providers plus kernel {@link AuthHandler}.
	 *
	 * @return provider manager for {@link AuthFilter}
	 */
	@Bean
	@SuppressWarnings("unchecked")
	public AuthenticationManager authenticationManager() {
		List<AuthenticationProvider> authProviders = new ArrayList<>();
		String applName = getApplicationName();
		List<String> otherAuthProviders = environment.getProperty(
				"mosip.security.authentication.provider.beans.list." + applName, List.class,
				Collections.emptyList());
		otherAuthProviders.forEach(beanName -> {
			try {
				if (Objects.nonNull(beanName) && !beanName.isEmpty()) {
					authProviders.add(
							applicationContext.getBean(beanName, AbstractUserDetailsAuthenticationProvider.class));
				}
			}
			catch (Exception ex) {
				// optional custom providers — same behaviour as kernel SecurityConfig
			}
		});
		authProviders.add(authProvider);
		return new ProviderManager(authProviders);
	}

	/**
	 * Kernel MOSIP auth filter validating JWT/cookie on every request except configured exclusions.
	 *
	 * @return {@link AuthFilter} registered before {@link UsernamePasswordAuthenticationFilter}
	 */
	@Bean
	public AbstractAuthenticationProcessingFilter authFilter() {
		RequestMatcher requestMatcher = AnyRequestMatcher.INSTANCE;
		AuthFilter filter = new AuthFilter(requestMatcher, noAuthenticationEndPoint, environment);
		filter.setAuthenticationManager(authenticationManager());
		filter.setAuthenticationSuccessHandler(new AuthSuccessHandler());
		return filter;
	}

	/**
	 * Disables servlet-container auto-registration of {@link AuthFilter}; Spring Security owns the filter chain.
	 *
	 * @param filter auth filter bean
	 * @return disabled {@link FilterRegistrationBean}
	 */
	@Bean
	public FilterRegistrationBean<AbstractAuthenticationProcessingFilter> registration(
			AbstractAuthenticationProcessingFilter filter) {
		FilterRegistrationBean<AbstractAuthenticationProcessingFilter> registration = new FilterRegistrationBean<>(
				filter);
		registration.setEnabled(false);
		return registration;
	}

	/**
	 * Primary HTTP security filter chain: stateless session, permit-all exclusions, auth filter, optional CORS/CSRF.
	 *
	 * @param http Spring Security builder
	 * @return built filter chain
	 * @throws Exception if security configuration fails
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		if (!isCSRFEnable) {
			http = http.csrf(csrf -> csrf.disable());
		}
		else {
			http.csrf(csrf -> csrf.ignoringRequestMatchers(antPathPatternsMatcher(csrfIgnoreUrls))
					.csrfTokenRepository(getCsrfTokenRepository()));
		}

		String[] exclusionPatterns = Stream.concat(
				Optional.ofNullable(noAuthenticationEndPoint.getGlobal()).map(GlobalEndPoint::getEndPoints)
						.map(List::stream).orElseGet(Stream::empty),
				Optional.ofNullable(noAuthenticationEndPoint.getService()).map(ServiceEndPoint::getEndPoints)
						.map(List::stream).orElseGet(Stream::empty))
				.toArray(String[]::new);

		http.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(antPathPatternsMatcher(exclusionPatterns)).permitAll()
				.anyRequest().authenticated());
		http.exceptionHandling(exception -> exception.authenticationEntryPoint(new KernelAuthEntryPoint()));
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.addFilterBefore(authFilter(), UsernamePasswordAuthenticationFilter.class);
		if (isCORSEnable) {
			http.addFilterBefore(new CorsFilter(origins), AuthFilter.class);
		}
		http.headers(headers -> {
			headers.cacheControl(Customizer.withDefaults());
			headers.frameOptions(frame -> frame.sameOrigin());
		});

		return http.build();
	}

	/**
	 * Resolves the first entry of {@code spring.application.name} for per-app security property keys.
	 *
	 * @return application name used in {@code mosip.security.*.{appName}} lookups
	 */
	private String getApplicationName() {
		String appNames = environment.getProperty("spring.application.name");
		if (appNames != null && !EmptyCheckUtils.isNullEmpty(appNames)) {
			return Stream.of(appNames.split(",")).collect(Collectors.toList()).get(0);
		}
		throw new RuntimeException("property spring.application.name is not found");
	}

	/**
	 * Cookie-based CSRF token repository when CSRF is enabled.
	 *
	 * @return CSRF repository with path {@code /}
	 */
	private CsrfTokenRepository getCsrfTokenRepository() {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookiePath("/");
		return repository;
	}

	/**
	 * Matches kernel auth exclusion URLs with Ant patterns (including multi-wildcard forms).
	 * Boot 4 requestMatchers(String...) uses PathPattern and rejects multiple {@code **}.
	 */
	private static RequestMatcher antPathPatternsMatcher(String... patterns) {
		if (patterns == null || patterns.length == 0) {
			return request -> false;
		}
		AntPathMatcher pathMatcher = new AntPathMatcher();
		List<RequestMatcher> matchers = Arrays.stream(patterns)
				.filter(StringUtils::hasText)
				.<RequestMatcher>map(pattern -> request -> pathMatcher.match(pattern, lookupPath(request)))
				.collect(Collectors.toList());
		if (matchers.isEmpty()) {
			return request -> false;
		}
		return new OrRequestMatcher(matchers);
	}

	/**
	 * Resolves servlet path for Ant-style matching (servlet path + path info, minus context path).
	 *
	 * @param request inbound HTTP request
	 * @return path used for {@link AntPathMatcher}
	 */
	private static String lookupPath(HttpServletRequest request) {
		String path = request.getServletPath();
		String pathInfo = request.getPathInfo();
		if (pathInfo != null) {
			path = path + pathInfo;
		}
		if (!StringUtils.hasText(path)) {
			path = request.getRequestURI();
			String contextPath = request.getContextPath();
			if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
				path = path.substring(contextPath.length());
			}
		}
		return path;
	}

	/**
	 * Returns HTTP 401 with body {@code UNAUTHORIZED} when authentication is missing or invalid.
	 */
	private static final class KernelAuthEntryPoint implements AuthenticationEntryPoint {

		@Override
		public void commence(HttpServletRequest request, HttpServletResponse response,
				AuthenticationException authException) throws IOException, ServletException {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED");
		}
	}
}