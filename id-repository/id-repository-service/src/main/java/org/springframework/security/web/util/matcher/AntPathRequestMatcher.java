package org.springframework.security.web.util.matcher;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Spring Security 7 removed {@code AntPathRequestMatcher}; kernel-auth {@code AuthFilter} still
 * instantiates it at runtime. This shim delegates to {@link AntPathMatcher} on the app classpath.
 */
public class AntPathRequestMatcher implements RequestMatcher {

	private final String pattern;

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	public AntPathRequestMatcher(String pattern) {
		this.pattern = pattern;
	}

	public AntPathRequestMatcher(String pattern, String httpMethod) {
		this.pattern = pattern;
	}

	@Override
	public boolean matches(HttpServletRequest request) {
		return pathMatcher.match(pattern, lookupPath(request));
	}

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

}
