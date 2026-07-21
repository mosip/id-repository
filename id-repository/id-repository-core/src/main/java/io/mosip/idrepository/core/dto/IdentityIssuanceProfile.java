package io.mosip.idrepository.core.dto;

import java.time.LocalDate;

import io.mosip.idrepository.core.builder.IdentityIssuanceProfileBuilder;
import lombok.Data;

/**
 * Anonymized before/after snapshot published when identity is issued or updated.
 *
 * <p>
 * Carries process name, event date, and {@link AnonymousProfile} pairs built by
 * {@link IdentityIssuanceProfileBuilder} for WebSub or audit/analytics
 * consumers. Does not include raw UIN or full identity JSON.
 * </p>
 *
 * <h2>Usage</h2>
 * <p>
 * Prefer {@link #builder()} which returns {@link IdentityIssuanceProfileBuilder}
 * for fluent construction from old/new identity documents and
 * {@link IdentityMapping} configuration.
 * </p>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@code IdentityIssuanceProfileBuilder}</li>
 *   <li>Identity lifecycle paths that publish issuance profiles</li>
 *   <li>Downstream analytics / WebSub subscribers</li>
 * </ul>
 *
 * @see AnonymousProfile
 * @see IdentityIssuanceProfileBuilder
 * @see IdentityMapping
 */
@Data
public class IdentityIssuanceProfile {

	/** Business process that triggered profiling (for example, NEW, UPDATE). */
	private String processName;

	/** Date on which the identity change occurred. */
	private LocalDate date;

	/** Anonymized profile captured before the identity change. */
	AnonymousProfile oldProfile;

	/** Anonymized profile captured after the identity change. */
	AnonymousProfile newProfile;

	/**
	 * Factory for fluent construction via {@link IdentityIssuanceProfileBuilder}.
	 *
	 * @return new builder instance
	 */
	public static IdentityIssuanceProfileBuilder builder() {
		return new IdentityIssuanceProfileBuilder();
	}
}
