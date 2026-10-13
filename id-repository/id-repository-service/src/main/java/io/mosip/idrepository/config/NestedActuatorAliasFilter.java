package io.mosip.idrepository.config;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Forwards legacy nested actuator URLs to the root {@code /actuator/**} endpoints.
 * <p>
 * The consolidated service uses {@code server.servlet.path=/}, so Spring Boot maps
 * management endpoints only under {@code /actuator/*}. Helm probes and apitest still
 * call historical paths such as {@code /idrepository/v1/identity/actuator/health}.
 * Without this alias those requests hit domain controllers and return
 * {@code IDR-IDC-003} (or similar) instead of actuator JSON.
 * </p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NestedActuatorAliasFilter extends OncePerRequestFilter {

	private static final Pattern NESTED_ACTUATOR = Pattern.compile(
			"^/(?:idrepository/v1(?:/identity)?|v1/credential(?:service|request))/actuator(/.*)?$");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String path = request.getRequestURI();
		String contextPath = request.getContextPath();
		if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
			path = path.substring(contextPath.length());
		}
		if (path.isEmpty()) {
			path = "/";
		}

		Matcher matcher = NESTED_ACTUATOR.matcher(path);
		if (matcher.matches()) {
			String suffix = matcher.group(1) == null ? "" : matcher.group(1);
			request.getRequestDispatcher("/actuator" + suffix).forward(request, response);
			return;
		}
		filterChain.doFilter(request, response);
	}
}
