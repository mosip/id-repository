package io.mosip.idrepository.core.dto;

import java.util.Map;

import lombok.Data;

/**
 * Generic WebSub message envelope wrapping a typed event payload.
 *
 * <p>
 * Standard publisher metadata ({@link #publisher}, {@link #topic},
 * {@link #publishedOn}) surrounds a domain-specific {@link #event} object and
 * optional {@link #data} map. Used for credentials, auth-type updates, and
 * other ID Repository WebSub publications.
 * </p>
 *
 * <h2>WebSub context</h2>
 * <p>
 * Typical topics include partner-scoped {@code {partnerId}/CREDENTIAL_ISSUED}
 * and {@code CREDENTIAL_STATUS_UPDATE}. The envelope is deserialized by
 * subscribers (including IDA) from WebSub callback bodies.
 * </p>
 *
 * <h2>Usage</h2>
 * <ul>
 *   <li>{@code EventModel<CredentialStatusUpdateEvent>} — status updates</li>
 *   <li>{@code EventModel<AuthTypeStatusEventDTO>} — auth-type lock changes</li>
 *   <li>{@code EventModel<CredentialServiceEventResponse>} — credential issued</li>
 * </ul>
 *
 * <h2>Consumers</h2>
 * <ul>
 *   <li>{@link io.mosip.idrepository.core.helper.IdRepoWebSubHelper}</li>
 *   <li>Credential and identity WebSub publishers</li>
 *   <li><strong>IDA</strong> and partner WebSub subscribers</li>
 * </ul>
 *
 * <h2>IDA stability</h2>
 * <p>
 * Keep envelope field names stable ({@code publisher}, {@code topic},
 * {@code publishedOn}, {@code event}, {@code data}). Nested event types have
 * their own IDA contracts; changing the envelope breaks all subscribers.
 * </p>
 *
 * @param <T> type of the domain event in {@link #event}
 * @author Nagarjuna
 * @see CredentialStatusUpdateEvent
 * @see AuthTypeStatusEventDTO
 * @see CredentialServiceEventResponse
 */
@Data
public class EventModel<T> {

	/** Identifier of the WebSub publisher (typically the originating service). */
	private String publisher;

	/** WebSub topic name the event was published on. */
	private String topic;

	/** ISO-8601 timestamp when the event was published. */
	private String publishedOn;

	/** Deserialized domain event payload. */
	private T event;

	/** Optional supplementary key-value data attached by the publisher. */
	private Map<String, Object> data;
}
