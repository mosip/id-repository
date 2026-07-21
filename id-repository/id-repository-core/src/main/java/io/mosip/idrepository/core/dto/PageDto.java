package io.mosip.idrepository.core.dto;

import java.util.List;

import org.springframework.data.domain.Sort;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic paginated result container for credential-request list APIs.
 *
 * <p>
 * Exposes page index, size, sort, totals, and the current page of typed
 * records. Typically parameterized with {@link CredentialRequestIdsDto} when
 * listing credential requests filtered by status.
 * </p>
 *
 * <h2>API context</h2>
 * <p>
 * Returned by credential-request get-request-ids (and similar) endpoints under
 * {@code /v1/credentialrequest/}. Page metadata mirrors Spring Data paging
 * concepts for partner/admin clients.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code CredentialRequestService#getRequestIds}</li>
 *   <li>Admin / partner UIs listing queue entries</li>
 *   <li><strong>IDA</strong> — may consume pages of {@link CredentialRequestIdsDto}</li>
 * </ul>
 *
 * @param <T> element type contained in {@link #data}
 * @see CredentialRequestIdsDto
 * @see io.mosip.idrepository.credential.request.service.CredentialRequestService#getRequestIds
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageDto<T> {

	/** Zero-based page index of this result set. */
	private int pageNo;

	/** Maximum number of items requested per page. */
	private int pageSize;

	/** Sort order applied to the underlying query. */
	private Sort sort;

	/** Total matching records across all pages. */
	private long totalItems;

	/** Total number of pages given {@link #pageSize} and {@link #totalItems}. */
	private int totalPages;

	/** Records for the current page. */
	private List<T> data;
}
