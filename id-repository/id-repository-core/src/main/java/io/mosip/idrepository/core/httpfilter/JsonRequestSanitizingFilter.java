package io.mosip.idrepository.core.httpfilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet filter that replaces invisible Unicode whitespace in JSON request bodies
 * before Jackson parses them.
 *
 * <p>
 * Copy-paste from Postman, browsers, or editors often inserts U+00A0 (non-breaking space)
 * or a UTF-8 BOM (U+FEFF), which causes failures such as
 * {@code JsonParseException: Unexpected character ('\u00a0' ...)}. This filter runs once
 * per request at high precedence, buffers the body, sanitizes it, and wraps the request
 * so downstream filters and controllers read the cleaned bytes.
 * </p>
 *
 * <h2>Order</h2>
 * <p>
 * {@link Order}{@code Ordered.HIGHEST_PRECEDENCE + 5} so sanitization happens early,
 * before most other filters and before Spring MVC argument resolution.
 * </p>
 *
 * <h2>Scope</h2>
 * <p>
 * Only requests whose {@code Content-Type} contains {@code application/json} (case
 * insensitive) are buffered and sanitized. All other content types pass through
 * unchanged.
 * </p>
 *
 * <h2>Sanitization rules</h2>
 * <ul>
 *   <li>Remove UTF-8 BOM ({@code \uFEFF})</li>
 *   <li>Replace non-breaking space ({@code \u00A0}) with a normal space ({@code ' '})</li>
 * </ul>
 * <p>
 * If nothing changes, the original byte array is reused (no re-encode).
 * </p>
 *
 * <h2>Usage</h2>
 * <p>
 * Registered automatically as a Spring {@code @Component}. No manual wiring required in
 * the consolidated {@code id-repository-service} deployable.
 * </p>
 *
 * @see OncePerRequestFilter
 * @see BaseIdRepoFilter
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class JsonRequestSanitizingFilter extends OncePerRequestFilter {

	/**
	 * Buffers and sanitizes JSON request bodies, then continues the filter chain.
	 * <p>
	 * Non-JSON requests are passed through without reading the body. JSON requests are
	 * wrapped in {@link CachedBodyRequest}; if sanitization changes the bytes, the
	 * cached body is replaced before {@code filterChain.doFilter}.
	 * </p>
	 *
	 * @param request     incoming HTTP request
	 * @param response    HTTP response
	 * @param filterChain remaining filter chain
	 * @throws ServletException if the chain fails
	 * @throws IOException      if the body cannot be read or written
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!isJsonRequest(request)) {
			filterChain.doFilter(request, response);
			return;
		}
		CachedBodyRequest wrapped = new CachedBodyRequest(request);
		byte[] sanitized = sanitizeJsonBody(wrapped.getCachedBody());
		if (!Arrays.equals(sanitized, wrapped.getCachedBody())) {
			wrapped.setCachedBody(sanitized);
		}
		filterChain.doFilter(wrapped, response);
	}

	/**
	 * Returns {@code true} when the request {@code Content-Type} indicates JSON.
	 *
	 * @param request incoming HTTP request
	 * @return {@code true} if content type is non-null and contains
	 *         {@code application/json} (case insensitive)
	 */
	private static boolean isJsonRequest(HttpServletRequest request) {
		String contentType = request.getContentType();
		return contentType != null && contentType.toLowerCase().contains("application/json");
	}

	/**
	 * Removes BOM and replaces non-breaking spaces in a UTF-8 JSON body.
	 * <p>
	 * Package-visible for unit tests. Returns the original {@code body} reference when
	 * no changes are needed.
	 * </p>
	 *
	 * @param body raw request body bytes; may be {@code null} or empty
	 * @return sanitized UTF-8 bytes, the original array if unchanged, or {@code body}
	 *         when null/empty
	 */
	static byte[] sanitizeJsonBody(byte[] body) {
		if (body == null || body.length == 0) {
			return body;
		}
		String text = new String(body, StandardCharsets.UTF_8);
		boolean changed = false;
		if (text.indexOf('\uFEFF') >= 0) {
			text = text.replace("\uFEFF", "");
			changed = true;
		}
		if (text.indexOf('\u00A0') >= 0) {
			text = text.replace('\u00A0', ' ');
			changed = true;
		}
		return changed ? text.getBytes(StandardCharsets.UTF_8) : body;
	}

	/**
	 * {@link HttpServletRequestWrapper} that caches the request body so it can be
	 * sanitized and re-read by downstream components.
	 * <p>
	 * The servlet input stream can normally be read only once; this wrapper stores the
	 * full body in memory and serves {@link #getInputStream()} /
	 * {@link #getReader()} from that cache.
	 * </p>
	 */
	private static final class CachedBodyRequest extends HttpServletRequestWrapper {

		/** Cached (possibly sanitized) request body bytes. */
		private byte[] cachedBody;

		/**
		 * Reads the entire original request body into {@link #cachedBody}.
		 *
		 * @param request original HTTP request
		 * @throws IOException if the input stream cannot be read
		 */
		CachedBodyRequest(HttpServletRequest request) throws IOException {
			super(request);
			try (InputStream inputStream = request.getInputStream()) {
				this.cachedBody = inputStream.readAllBytes();
			}
		}

		/**
		 * @return current cached body bytes (may be the original or sanitized array)
		 */
		byte[] getCachedBody() {
			return cachedBody;
		}

		/**
		 * Replaces the cached body after sanitization.
		 *
		 * @param cachedBody new body bytes to serve from {@link #getInputStream()}
		 */
		void setCachedBody(byte[] cachedBody) {
			this.cachedBody = cachedBody;
		}

		/**
		 * Returns a {@link ServletInputStream} over the cached body.
		 * <p>
		 * Supports repeated reads for the same request. Async {@link ReadListener} is
		 * not used (no-op).
		 * </p>
		 *
		 * @return stream backed by {@link #cachedBody}
		 */
		@Override
		public ServletInputStream getInputStream() {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(cachedBody);
			return new ServletInputStream() {
				@Override
				public int read() {
					return inputStream.read();
				}

				@Override
				public boolean isFinished() {
					return inputStream.available() == 0;
				}

				@Override
				public boolean isReady() {
					return true;
				}

				@Override
				public void setReadListener(ReadListener readListener) {
					// not used
				}
			};
		}

		/**
		 * Returns a UTF-8 {@link BufferedReader} over {@link #getInputStream()}.
		 *
		 * @return reader for the cached body
		 */
		@Override
		public BufferedReader getReader() {
			return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
		}
	}
}
